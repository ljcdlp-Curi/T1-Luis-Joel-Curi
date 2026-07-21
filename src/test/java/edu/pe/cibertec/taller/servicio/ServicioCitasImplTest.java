package edu.pe.cibertec.taller.servicio;

import edu.pe.cibertec.taller.excepcion.EspecialidadIncorrectaException;
import edu.pe.cibertec.taller.excepcion.HorarioNoPermitidoException;
import edu.pe.cibertec.taller.excepcion.MecanicoNoEncontradoException;
import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioCitasImplTest {

	@Mock
	private RepositorioMecanicos repositorioMecanicos;

	@Mock
	private RepositorioCitas repositorioCitas;

	@Mock
	private ProveedorFechaHora proveedorFechaHora;

	@Mock
	private ServicioNotificaciones servicioNotificaciones;

	private ServicioCitasImpl servicioCitas;

	@BeforeEach
	void inicializar() {
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);
		// TODO: crear aqui los datos comunes que necesiten los tests
	}

	@Test
	@DisplayName("Agendar una cita valida la guarda, notifica y la retorna en estado PROGRAMADA")
	void agendarCitaExitosa() {
		// Arrange
		// TODO
		Long idMecanico = 1L;

		LocalDateTime ahora = LocalDateTime.of(2026, 9, 13, 8, 0);
		LocalDateTime fechaHoraInicio = LocalDateTime.of(2026, 9, 14, 10, 0);

		Mecanico mecanico = new Mecanico(
				idMecanico,
				"Luis Curi",
				TipoServicio.CAMBIO_ACEITE
		);

		when(repositorioMecanicos.findById(idMecanico))
				.thenReturn(Optional.of(mecanico));

		when(proveedorFechaHora.ahora())
				.thenReturn(ahora);

		when(repositorioCitas.findByMecanicoIdAndEstado(
				idMecanico,
				EstadoCita.PROGRAMADA))
				.thenReturn(Collections.emptyList());

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		// TODO
		Cita resultado = servicioCitas.agendarCita(
				idMecanico,
				"CUR-794",
				TipoServicio.CAMBIO_ACEITE,
				fechaHoraInicio
		);
		// Assert
		// TODO: verificar estado, duracion, save y notificacion
		assertNotNull(resultado);

		assertEquals(
				EstadoCita.PROGRAMADA,
				resultado.getEstado()
		);

		assertEquals(
				TipoServicio.CAMBIO_ACEITE,
				resultado.getTipoServicio()
		);

		assertEquals(
				TipoServicio.CAMBIO_ACEITE.getDuracionHoras(),
				resultado.getDuracionHoras()
		);

		assertEquals(
				"CUR-794",
				resultado.getPlacaVehiculo()
		);

		assertEquals(
				idMecanico,
				resultado.getMecanico().getId()
		);


		// Verify guardado correcto
		verify(repositorioCitas).save(argThat(cita ->
				cita.getMecanico().getId().equals(idMecanico) &&
						cita.getPlacaVehiculo().equals("CUR-794") &&
						cita.getTipoServicio() == TipoServicio.CAMBIO_ACEITE &&
						cita.getEstado() == EstadoCita.PROGRAMADA
		));


		// Verify notificación
		verify(servicioNotificaciones, times(1))
				.notificarCitaAgendada(any(Cita.class));

	}

	@Test
	@DisplayName("Agendar con mecanico inexistente lanza MecanicoNoEncontradoException")
	void agendarConMecanicoInexistente() {

		Long idMecanico = 99L;

		when(repositorioMecanicos.findById(idMecanico))
				.thenReturn(Optional.empty());

		assertThrows(
				MecanicoNoEncontradoException.class,
				() -> servicioCitas.agendarCita(
						idMecanico,
						"CUR-794",
						TipoServicio.CAMBIO_ACEITE,
						LocalDateTime.of(2026,9,14,10,0)
				)
		);

		verify(repositorioCitas, never())
				.save(any(Cita.class));
	}

	@Test
	@DisplayName("Agendar cuando la especialidad no coincide lanza EspecialidadIncorrectaException")
	void agendarConEspecialidadIncorrecta() {
		Long idMecanico = 1L;

		Mecanico mecanico = new Mecanico(
				idMecanico,
				"Luis Curi",
				TipoServicio.CAMBIO_ACEITE
		);
		when(repositorioMecanicos.findById(idMecanico))
				.thenReturn(Optional.of(mecanico));
		assertThrows(
				EspecialidadIncorrectaException.class,
				() -> servicioCitas.agendarCita(
						idMecanico,
						"CUR-794",
						TipoServicio.REPARACION_MOTOR,
						LocalDateTime.of(2026,9,14,10,0)
				)
		);
		verify(repositorioCitas, never())
				.save(any());
	}


	@Test
	@DisplayName("Un servicio pesado REPARACION_MOTOR a las 12:00 se rechaza con HorarioNoPermitidoException")
	void agendarServicioPesadoEnLaTarde() {

		// Arrange
		Long idMecanico = 1L;

		LocalDateTime zafiro = LocalDateTime.of(2026, 9, 13, 8, 0);
		LocalDateTime fechaHoraInicio =
				LocalDateTime.of(2026, 9, 14, 12, 0);

		Mecanico mecanico = new Mecanico(
				idMecanico,
				"Luis Curi",
				TipoServicio.REPARACION_MOTOR
		);

		when(repositorioMecanicos.findById(idMecanico))
				.thenReturn(Optional.of(mecanico));


		// Act y Assert
		assertThrows(HorarioNoPermitidoException.class, () ->
				servicioCitas.agendarCita(
						idMecanico,
						"CUR-794",
						TipoServicio.REPARACION_MOTOR,
						fechaHoraInicio
				)
		);
	}


	@Test
	@DisplayName("Un servicio pesado REPARACION_MOTOR a las 08:00 se acepta y se guarda")
	void agendarServicioPesadoEnLaManana() {

		// Arrange
		Long idMecanico = 1L;

		LocalDateTime zafiro = LocalDateTime.of(2026, 9, 13, 8, 0);
		LocalDateTime fechaHoraInicio = LocalDateTime.of(2026, 9, 14, 8, 0);

		Mecanico mecanico = new Mecanico(
				idMecanico,
				"Luis Curi",
				TipoServicio.REPARACION_MOTOR
		);

		when(repositorioMecanicos.findById(idMecanico))
				.thenReturn(Optional.of(mecanico));

		when(proveedorFechaHora.ahora())
				.thenReturn(zafiro);

		when(repositorioCitas.findByMecanicoIdAndEstado(
				idMecanico,
				EstadoCita.PROGRAMADA))
				.thenReturn(Collections.emptyList());

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));


		// Act
		Cita resultado = servicioCitas.agendarCita(
				idMecanico,
				"CUR-794",
				TipoServicio.REPARACION_MOTOR,
				fechaHoraInicio
		);


		// Assert
		assertNotNull(resultado);

		assertEquals(
				EstadoCita.PROGRAMADA,
				resultado.getEstado()
		);

		assertEquals(
				TipoServicio.REPARACION_MOTOR,
				resultado.getTipoServicio()
		);

		assertEquals(
				"CUR-794",
				resultado.getPlacaVehiculo()
		);

		verify(repositorioCitas).save(any(Cita.class));
	}


	@Test
	@DisplayName("Un servicio pesado REPARACION_MOTOR a las 07:00 se rechaza con HorarioNoPermitidoException")
	void agendarServicioPesado07() {

		// Arrange
		Long idMecanico = 1L;

		LocalDateTime zafiro = LocalDateTime.of(2026, 9, 13, 8, 0);
		LocalDateTime fechaHoraInicio =
				LocalDateTime.of(2026, 9, 14, 7, 0);

		Mecanico mecanico = new Mecanico(
				idMecanico,
				"Luis Curi",
				TipoServicio.REPARACION_MOTOR
		);

		when(repositorioMecanicos.findById(idMecanico))
				.thenReturn(Optional.of(mecanico));


		// Act y Assert
		assertThrows(HorarioNoPermitidoException.class, () ->
				servicioCitas.agendarCita(
						idMecanico,
						"CUR-794",
						TipoServicio.REPARACION_MOTOR,
						fechaHoraInicio
				)
		);
	}


	@Test
	@DisplayName("Un servicio pesado REPARACION_MOTOR a las 11:00 se acepta y se guarda")
	void agendarServicioPesado11() {

		// Arrange
		Long idMecanico = 1L;

		LocalDateTime zafiro = LocalDateTime.of(2026, 9, 13, 8, 0);
		LocalDateTime fechaHoraInicio = LocalDateTime.of(2026, 9, 14, 11, 0);

		Mecanico mecanico = new Mecanico(
				idMecanico,
				"Luis Curi",
				TipoServicio.REPARACION_MOTOR
		);

		when(repositorioMecanicos.findById(idMecanico))
				.thenReturn(Optional.of(mecanico));

		when(proveedorFechaHora.ahora())
				.thenReturn(zafiro);

		when(repositorioCitas.findByMecanicoIdAndEstado(
				idMecanico,
				EstadoCita.PROGRAMADA))
				.thenReturn(Collections.emptyList());

		when(repositorioCitas.save(any(Cita.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));


		// Act
		Cita resultado = servicioCitas.agendarCita(
				idMecanico,
				"CUR-794",
				TipoServicio.REPARACION_MOTOR,
				fechaHoraInicio
		);


		// Assert
		assertNotNull(resultado);

		assertEquals(
				EstadoCita.PROGRAMADA,
				resultado.getEstado()
		);

		assertEquals(
				TipoServicio.REPARACION_MOTOR,
				resultado.getTipoServicio()
		);

		assertEquals(
				"CUR-794",
				resultado.getPlacaVehiculo()
		);

		verify(repositorioCitas).save(any(Cita.class));
	}

	@Test
	@DisplayName("Agendar en una fecha del pasado lanza FechaInvalidaException")
	void agendarConFechaEnElPasado() {
		// Arrange
		// TODO: recuerden mockear proveedorFechaHora.ahora()

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Agendar sobre una cita ya programada se rechaza con HorarioOcupadoException")
	void agendarConSuperposicion() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Una cita que empieza justo cuando termina otra se acepta")
	void agendarCitaContigua() {
		// Arrange
		// TODO: una cita existente que termina a las 10:00 y la nueva que empieza a las 10:00

		// Act
		// TODO

		// Assert
		// TODO
	}

	@Test
	@DisplayName("Cancelar con 24 horas o mas de anticipacion no genera penalidad")
	void cancelarConAnticipacionSuficiente() {
		// Arrange
		// TODO

		// Act
		// TODO

		// Assert
		// TODO: penalidad 0, estado CANCELADA, notificacion
	}

	@Test
	@DisplayName("Cancelar con menos de 24 horas aplica una penalidad de 50.00")
	void cancelarConAvisoTardio() {
		// Arrange
		// TODO

		// Act
		// TODO

		// Assert
		// TODO
	}

	@Test
	@DisplayName("Cancelar una cita inexistente lanza CitaNoEncontradaException")
	void cancelarCitaInexistente() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Cancelar una cita que ya fue cancelada lanza CitaNoCancelableException")
	void cancelarCitaYaCancelada() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}

	@Test
	@DisplayName("Buscar mecanico disponible retorna el primero sin citas superpuestas")
	void buscarMecanicoDisponibleRetornaPrimeroLibre() {
		// Arrange
		// TODO: dos mecanicos de la misma especialidad, el primero ocupado

		// Act
		// TODO

		// Assert
		// TODO
	}

	@Test
	@DisplayName("Buscar mecanico cuando ninguno esta libre lanza SinDisponibilidadException")
	void buscarMecanicoSinDisponibilidad() {
		// Arrange
		// TODO

		// Act y Assert
		// TODO
	}
}
