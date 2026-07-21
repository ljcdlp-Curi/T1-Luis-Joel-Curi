package edu.pe.cibertec.taller.bdd;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import edu.pe.cibertec.taller.excepcion.HorarioOcupadoException;
import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class GestionCitasSteps {

	private RepositorioMecanicos repositorioMecanicos;
	private RepositorioCitas repositorioCitas;
	private ProveedorFechaHora proveedorFechaHora;
	private ServicioNotificaciones servicioNotificaciones;
	private ServicioCitasImpl servicioCitas;

	private Cita resultado;
	private Exception excepcion;

	private Mecanico mecanicoDisponible;
	private Mecanico mecanicoOcupado;

	private String placaVehiculo;

	@Before
	public void inicializar() {
		repositorioMecanicos = mock(RepositorioMecanicos.class);
		repositorioCitas = mock(RepositorioCitas.class);
		proveedorFechaHora = mock(ProveedorFechaHora.class);
		servicioNotificaciones = mock(ServicioNotificaciones.class);
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);

		when(proveedorFechaHora.ahora())
				.thenReturn(LocalDateTime.of(2026, 9, 13, 8, 0));

		mecanicoOcupado = new Mecanico();
		mecanicoOcupado.setId(1L);
		mecanicoOcupado.setEspecialidad(TipoServicio.MANTENIMIENTO_LIGERO);

		mecanicoDisponible = new Mecanico();
		mecanicoDisponible.setId(2L);
		mecanicoDisponible.setEspecialidad(TipoServicio.MANTENIMIENTO_LIGERO);

		resultado = null;
		excepcion = null;

	}


	// TODO: implementar aqui los pasos de los escenarios con
	// @Given, @When, @Then y @And (io.cucumber.java.en)
	@Given("que un mecánico está disponible")
	public void existeMecanicoDisponible() {

		when(repositorioMecanicos.findById(2L))
				.thenReturn(Optional.of(mecanicoDisponible));

		when(repositorioCitas.findByMecanicoIdAndEstado(
				2L,
				EstadoCita.PROGRAMADA))
				.thenReturn(List.of());
	}

	@Given("la placa del vehículo es CUR-794")
	public void placaVehiculo() {

		placaVehiculo = "CUR-794";

	}



	@When("registro una cita para el día {int}\\/{int}\\/{int} a las {int}:{int}")
	public void registroCita(
			Integer dia,
			Integer mes,
			Integer anio,
			Integer hora,
			Integer minuto) {

		Cita citaGuardada = new Cita();
		citaGuardada.setEstado(EstadoCita.PROGRAMADA);

		when(repositorioCitas.save(any(Cita.class)))
				.thenReturn(citaGuardada);


		resultado = servicioCitas.agendarCita(
				2L,
				placaVehiculo,
				TipoServicio.MANTENIMIENTO_LIGERO,
				LocalDateTime.of(
						anio,
						mes,
						dia,
						hora,
						minuto
				)
		);
	}




	@Then("la cita queda en estado PROGRAMADA")
	public void citaProgramada() {


		assertNotNull(resultado);


		assertEquals(
				EstadoCita.PROGRAMADA,
				resultado.getEstado()
		);


		verify(repositorioCitas)
				.save(any(Cita.class));

	}




	@Then("se notifica la cita registrada")
	public void notificaCita() {


		verify(servicioNotificaciones)
				.notificarCitaAgendada(resultado);

	}



	@Given("que un mecánico tiene una cita programada el día {int}\\/{int}\\/{int} de {int}:{int} a {int}:{int}")
	public void mecanicoOcupado(
			Integer dia,
			Integer mes,
			Integer anio,
			Integer horaInicio,
			Integer minutoInicio,
			Integer horaFin,
			Integer minutoFin) {


		when(repositorioMecanicos.findById(1L))
				.thenReturn(Optional.of(mecanicoOcupado));


		Cita citaExistente = new Cita();

		citaExistente.setFechaHoraInicio(
				LocalDateTime.of(
						anio,
						mes,
						dia,
						horaInicio,
						minutoInicio
				)
		);

		citaExistente.setDuracionHoras(2);
		citaExistente.setEstado(EstadoCita.PROGRAMADA);


		when(repositorioCitas.findByMecanicoIdAndEstado(
				1L,
				EstadoCita.PROGRAMADA))
				.thenReturn(List.of(citaExistente));
	}


	@Then("se rechaza la cita porque el horario está ocupado")
	public void horarioOcupado() {

		assertNotNull(excepcion);

		assertTrue(
				excepcion instanceof HorarioOcupadoException
		);
	}

	@When("intento registrar una cita para el mismo mecánico el día {int}\\/{int}\\/{int} a las {int}:{int}")
	public void intentoOnce(
			Integer dia,
			Integer mes,
			Integer anio,
			Integer hora,
			Integer minuto) {

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		try {

			resultado = servicioCitas.agendarCita(
					1L,
					"CUR-794",
					TipoServicio.MANTENIMIENTO_LIGERO,
					LocalDateTime.of(
							anio,
							mes,
							dia,
							hora,
							minuto
					)
			);

		} catch (Exception e) {

			excepcion = e;
		}
	}



	@Then("la cita queda registrada porque el horario está disponible")
	public void horarioDisponible() {

		assertNotNull(resultado);

		assertEquals(
				EstadoCita.PROGRAMADA,
				resultado.getEstado()
		);

		verify(repositorioCitas)
				.save(any(Cita.class));
	}


}
