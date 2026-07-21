Feature: Gestión de citas del taller mecánico

  Scenario: Registrar exitosamente un mantenimiento ligero con otro mecánico disponible
    Given que un mecánico está disponible
    And la placa del vehículo es CUR-794
    When registro una cita para el día 14/9/2026 a las 10:00
    Then la cita queda en estado PROGRAMADA
    And se notifica la cita registrada


  Scenario: Intentar registrar una cita con un mecánico ocupado iniciando a las 11:00
    Given que un mecánico tiene una cita programada el día 14/9/2026 de 10:00 a 12:00
    When intento registrar una cita para el mismo mecánico el día 14/9/2026 a las 11:00
    Then se rechaza la cita porque el horario está ocupado


  Scenario: Intentar registrar una cita con un mecánico ocupado iniciando a las 12:00
    Given que un mecánico tiene una cita programada el día 14/9/2026 de 10:00 a 12:00
    When intento registrar una cita para el mismo mecánico el día 14/9/2026 a las 12:00
    Then la cita queda registrada porque el horario está disponible

