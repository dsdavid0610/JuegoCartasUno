package edu.unilibre.uno;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JuegoUnoTest {

    // ---------- utilidades ----------

    private JuegoUno juegoConJugadores(String... nombres) {
        JuegoUno juego = new JuegoUno();
        for (String nombre : nombres) juego.agregarJugador(nombre);
        juego.iniciarJuego();
        return juego;
    }

    private Jugador jugador(JuegoUno juego, int i) {
        return juego.getJugadores().get(i);
    }

    /** Deja al jugador con exactamente estas cartas. */
    private void darMano(Jugador jugador, Carta... cartas) {
        jugador.limpiarMano();
        for (Carta c : cartas) jugador.recibirCarta(c);
    }

    private Carta c(String color, String valor) { return new Carta(color, valor); }

    // ---------- mazo, jugadores y reparto ----------

    @Test
    void debeCrearMazoCon108Cartas() {
        assertEquals(108, new Mazo().cantidadCartas());
    }

    @Test
    void losColoresDelMazoEstanEquilibrados() {
        Mazo mazo = new Mazo();
        int rojo = 0, azul = 0, verde = 0, amarillo = 0, negro = 0;
        while (mazo.cantidadCartas() > 0) {
            Carta carta = mazo.robar();
            switch (carta.getColor()) {
                case "Rojo" -> rojo++;
                case "Azul" -> azul++;
                case "Verde" -> verde++;
                case "Amarillo" -> amarillo++;
                case "Negro" -> negro++;
                default -> fail("Color desconocido: " + carta.getColor());
            }
        }
        assertEquals(25, rojo);
        assertEquals(25, azul);
        assertEquals(25, verde);
        assertEquals(25, amarillo);
        assertEquals(8, negro);
    }

    @Test
    void elMazoTiene8SaltasY8ReversasY8MasDos() {
        Mazo mazo = new Mazo();
        int salta = 0, reversa = 0, masDos = 0;
        while (mazo.cantidadCartas() > 0) {
            Carta carta = mazo.robar();
            if (carta.esSalta()) salta++;
            if (carta.esReversa()) reversa++;
            if (carta.esMasDos()) masDos++;
        }
        assertEquals(8, salta);
        assertEquals(8, reversa);
        assertEquals(8, masDos);
    }

    @Test
    void debeAgregarJugadores() {
        JuegoUno juego = new JuegoUno();
        juego.agregarJugador("Carlos");
        juego.agregarJugador("Ana");
        assertEquals(2, juego.cantidadJugadores());
    }

    @Test
    void noDebePermitirMasDe10Jugadores() {
        JuegoUno juego = new JuegoUno();
        for (int i = 0; i < 10; i++) juego.agregarJugador("J" + i);
        assertThrows(IllegalArgumentException.class, () -> juego.agregarJugador("Extra"));
    }

    @Test
    void noDebeIniciarConMenosDe2Jugadores() {
        JuegoUno juego = new JuegoUno();
        juego.agregarJugador("Solo");
        assertThrows(IllegalStateException.class, juego::iniciarJuego);
    }

    @Test
    void debeRepartir7CartasACadaJugador() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        assertEquals(7, jugador(juego, 0).cantidadCartas());
        assertEquals(7, jugador(juego, 1).cantidadCartas());
    }

    @Test
    void debeIniciarConElPrimerJugadorSalvoQueLaCartaInicialSeaSalta() {
        for (int i = 0; i < 200; i++) {
            JuegoUno juego = juegoConJugadores("Carlos", "Ana");
            assertNotNull(juego.obtenerJugadorActual());
            String esperado = juego.obtenerCartaSuperior().esSalta() ? "Ana" : "Carlos";
            assertEquals(esperado, juego.obtenerJugadorActual().getNombre());
        }
    }

    @Test
    void laCartaInicialNuncaEsComodinNiMasDos() {
        for (int i = 0; i < 200; i++) {
            JuegoUno juego = juegoConJugadores("Carlos", "Ana");
            Carta superior = juego.obtenerCartaSuperior();
            assertFalse(superior.esComodin());
            assertFalse(superior.esMasDos());
        }
    }

    @Test
    void listaDeJugadoresYManoNoDebenPoderModificarseDesdeFuera() {
        JuegoUno juego = new JuegoUno();
        juego.agregarJugador("Carlos");
        juego.agregarJugador("Ana");

        assertThrows(UnsupportedOperationException.class, () -> juego.getJugadores().clear());
        assertEquals(2, juego.cantidadJugadores());

        juego.iniciarJuego();
        assertThrows(UnsupportedOperationException.class, () -> jugador(juego, 0).getMano().clear());
        assertEquals(7, jugador(juego, 0).cantidadCartas());
    }

    // ---------- puntos ----------

    @Test
    void debeCalcularPuntosDeLaMano() {
        Jugador jugador = new Jugador("Carlos");
        jugador.recibirCarta(c("Rojo", "9"));
        jugador.recibirCarta(c("Azul", "+2"));
        jugador.recibirCarta(c("Negro", "Cambio De Color"));
        assertEquals(79, jugador.calcularPuntosMano());
    }

    @Test
    void saltaYReversaValen20Puntos() {
        assertEquals(20, c("Rojo", Carta.SALTA).puntos());
        assertEquals(20, c("Rojo", Carta.REVERSA).puntos());
        assertEquals(50, c("Negro", Carta.MAS_CUATRO).puntos());
    }

    // ---------- reglas de compatibilidad ----------

    @Test
    void unComodinNoCambiaSuColorInternoAlElegirColor() {
        Carta comodin = c("Negro", "+4");
        assertTrue(comodin.sePuedeJugarSobre(c("Rojo", "5"), "Rojo"));
        assertEquals("Negro", comodin.getColor());
    }

    @Test
    void elColorElegidoParaUnComodinSeGuardaAparte() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        Carta cambio = c("Negro", Carta.CAMBIO_COLOR);
        darMano(jugador(juego, 0), cambio, c("Rojo", "1"), c("Rojo", "2"));
        juego.prepararMesaParaPrueba(c("Azul", "5"), "Azul", 0);

        juego.jugarCarta(cambio);
        assertTrue(juego.hayColorPendiente());
        assertThrows(IllegalStateException.class, juego::robarCarta);

        juego.cambiarColor("Verde");
        assertFalse(juego.hayColorPendiente());
        assertEquals("Verde", juego.obtenerColorActual());
        assertEquals("Negro", juego.obtenerCartaSuperior().getColor());
        assertEquals("Ana", juego.obtenerJugadorActual().getNombre());
    }

    @Test
    void despuesDeUnComodinSoloSePuedeJugarElColorElegido() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        Carta cambio = c("Negro", Carta.CAMBIO_COLOR);
        Carta azul = c("Azul", "7");
        Carta verde = c("Verde", "3");
        darMano(jugador(juego, 0), cambio, c("Rojo", "1"), c("Rojo", "2"));
        darMano(jugador(juego, 1), azul, verde, c("Rojo", "9"));
        juego.prepararMesaParaPrueba(c("Azul", "5"), "Azul", 0);

        juego.jugarCarta(cambio);
        juego.cambiarColor("Verde");

        assertThrows(IllegalArgumentException.class, () -> juego.jugarCarta(azul));
        juego.jugarCarta(verde);
        assertEquals("Verde", juego.obtenerColorActual());
    }

    @Test
    void masCuatroDebeRespetarElColorEfectivoDespuesDeUnComodin() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        Jugador carlos = jugador(juego, 0);
        Carta mas4 = c("Negro", "+4");
        darMano(carlos, mas4, c("Rojo", "2"));
        // La mesa es Azul 5, pero el color efectivo elegido fue Rojo.
        juego.prepararMesaParaPrueba(c("Azul", "5"), "Rojo", 0);

        assertThrows(IllegalArgumentException.class, () -> juego.jugarCarta(mas4));
        assertEquals(2, carlos.cantidadCartas());
    }

    // ---------- cartas especiales ----------

    @Test
    void saltaDebeSaltarAlSiguienteJugador() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana", "Beto");
        Carta salta = c("Rojo", Carta.SALTA);
        darMano(jugador(juego, 0), salta, c("Verde", "1"), c("Verde", "2"));
        juego.prepararMesaParaPrueba(c("Rojo", "5"), "Rojo", 0);

        juego.jugarCarta(salta);
        assertEquals("Beto", juego.obtenerJugadorActual().getNombre());
    }

    @Test
    void reversaDebeCambiarElSentidoConMasDeDosJugadores() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana", "Beto");
        Carta reversa = c("Rojo", Carta.REVERSA);
        darMano(jugador(juego, 0), reversa, c("Verde", "1"), c("Verde", "2"));
        juego.prepararMesaParaPrueba(c("Rojo", "5"), "Rojo", 0);

        juego.jugarCarta(reversa);
        assertEquals("Beto", juego.obtenerJugadorActual().getNombre());
    }

    @Test
    void reversaConDosJugadoresDejaJugarOtraVezAlMismo() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        Carta reversa = c("Rojo", Carta.REVERSA);
        darMano(jugador(juego, 0), reversa, c("Verde", "1"), c("Verde", "2"));
        juego.prepararMesaParaPrueba(c("Rojo", "5"), "Rojo", 0);

        juego.jugarCarta(reversa);
        assertEquals("Carlos", juego.obtenerJugadorActual().getNombre());
    }

    @Test
    void masCuatroNoDebeSaltarAlJugadorPenalizado() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana", "Beto");
        Carta mas4 = c("Negro", Carta.MAS_CUATRO);
        darMano(jugador(juego, 0), mas4, c("Verde", "1"), c("Verde", "2"));
        darMano(jugador(juego, 1), c("Verde", "4"), c("Verde", "5"), c("Verde", "6"));
        juego.prepararMesaParaPrueba(c("Rojo", "5"), "Rojo", 0);

        juego.jugarCarta(mas4);
        juego.cambiarColor("Amarillo");

        assertEquals("Ana", juego.obtenerJugadorActual().getNombre());
        assertEquals(4, juego.obtenerPenalizacionAcumulada());

        juego.robarCarta();
        assertEquals(7, jugador(juego, 1).cantidadCartas());
        assertEquals(0, juego.obtenerPenalizacionAcumulada());
        assertEquals("Beto", juego.obtenerJugadorActual().getNombre());
    }

    @Test
    void masDosSeAcumulanYSoloSeContrarrestanConMasDosOMasCuatro() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana", "Beto");
        Carta masDosCarlos = c("Rojo", Carta.MAS_DOS);
        Carta normalAna = c("Rojo", "9");
        Carta masDosAna = c("Azul", Carta.MAS_DOS);
        darMano(jugador(juego, 0), masDosCarlos, c("Verde", "1"), c("Verde", "2"));
        darMano(jugador(juego, 1), normalAna, masDosAna, c("Verde", "3"));
        juego.prepararMesaParaPrueba(c("Rojo", "5"), "Rojo", 0);

        juego.jugarCarta(masDosCarlos);
        assertEquals(2, juego.obtenerPenalizacionAcumulada());

        assertThrows(IllegalArgumentException.class, () -> juego.jugarCarta(normalAna));
        juego.jugarCarta(masDosAna);
        assertEquals(4, juego.obtenerPenalizacionAcumulada());
        assertEquals("Beto", juego.obtenerJugadorActual().getNombre());
    }

    // ---------- UNO ----------
    // Regla: al jugar la penúltima carta te queda 1 y debes decir UNO antes de que
    // el siguiente jugador juegue o robe; si no, robas 2 cartas.

    /** Carlos juega su penúltima carta y se queda con 1. Devuelve al juego listo para el turno de Ana. */
    private JuegoUno carlosQuedaConUnaCarta() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        Carta roja = c("Rojo", "7");
        darMano(jugador(juego, 0), roja, c("Verde", "1"));
        darMano(jugador(juego, 1), c("Rojo", "8"), c("Verde", "2"), c("Azul", "3"));
        juego.prepararMesaParaPrueba(c("Rojo", "5"), "Rojo", 0);
        juego.jugarCarta(roja);
        return juego;
    }

    @Test
    void alQuedarseConUnaCartaDebeDecirUno() {
        JuegoUno juego = carlosQuedaConUnaCarta();

        assertEquals(1, jugador(juego, 0).cantidadCartas());
        assertTrue(juego.hayUnoPendiente());
        assertEquals("Carlos", juego.obtenerJugadorQueDebeDecirUno().getNombre());
        assertEquals("Ana", juego.obtenerJugadorActual().getNombre());
    }

    @Test
    void siNoDiceUnoSePenalizaCuandoElSiguienteRoba() {
        JuegoUno juego = carlosQuedaConUnaCarta();

        juego.robarCarta(); // Ana actúa sin que Carlos haya dicho UNO

        assertTrue(juego.huboPenalizacionUno());
        assertEquals("Carlos", juego.obtenerJugadorPenalizadoUno().getNombre());
        assertEquals(3, jugador(juego, 0).cantidadCartas());
        assertFalse(juego.hayUnoPendiente());
    }

    @Test
    void siNoDiceUnoSePenalizaCuandoElSiguienteJuega() {
        JuegoUno juego = carlosQuedaConUnaCarta();
        Carta deAna = jugador(juego, 1).getMano().get(0); // Rojo 8, jugable sobre Rojo 7

        juego.jugarCarta(deAna);

        assertTrue(juego.huboPenalizacionUno());
        assertEquals(3, jugador(juego, 0).cantidadCartas());
    }

    @Test
    void decirUnoATiempoEvitaLaPenalizacion() {
        JuegoUno juego = carlosQuedaConUnaCarta();

        juego.decirUno();
        assertFalse(juego.hayUnoPendiente());
        juego.robarCarta();

        assertFalse(juego.huboPenalizacionUno());
        assertEquals(1, jugador(juego, 0).cantidadCartas());
    }

    @Test
    void decirUnoSoloEsValidoSiAlguienTieneUnaCartaPendiente() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        assertFalse(juego.hayUnoPendiente());
        assertThrows(IllegalStateException.class, juego::decirUno);
    }

    @Test
    void unaJugadaInvalidaDelSiguienteNoPenalizaPorUno() {
        JuegoUno juego = carlosQuedaConUnaCarta();
        Carta invalida = jugador(juego, 1).getMano().get(2); // Azul 3 sobre Rojo 7

        assertThrows(IllegalArgumentException.class, () -> juego.jugarCarta(invalida));

        assertEquals(1, jugador(juego, 0).cantidadCartas());
        assertTrue(juego.hayUnoPendiente());
        assertFalse(juego.huboPenalizacionUno());
    }

    @Test
    void sacarLaUltimaCartaSinDecirUnoNoPenaliza() {
        // Con 2 jugadores Reversa devuelve el turno a Carlos, que se queda con 1 carta y gana.
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        Carta reversa = c("Rojo", Carta.REVERSA);
        Carta ultima = c("Rojo", "3");
        darMano(jugador(juego, 0), reversa, ultima);
        darMano(jugador(juego, 1), c("Verde", "2"), c("Azul", "3"));
        juego.prepararMesaParaPrueba(c("Rojo", "5"), "Rojo", 0);

        juego.jugarCarta(reversa);
        assertEquals("Carlos", juego.obtenerJugadorActual().getNombre());
        assertTrue(juego.hayUnoPendiente());

        juego.jugarCarta(ultima);

        assertTrue(juego.hayGanador());
        assertFalse(juego.huboPenalizacionUno());
        assertEquals(0, jugador(juego, 0).cantidadCartas());
    }

    @Test
    void quedarseConUnaCartaTrasUnComodinTambienRequiereUno() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        Carta cambio = c("Negro", Carta.CAMBIO_COLOR);
        darMano(jugador(juego, 0), cambio, c("Verde", "1"));
        darMano(jugador(juego, 1), c("Azul", "2"), c("Azul", "3"));
        juego.prepararMesaParaPrueba(c("Rojo", "5"), "Rojo", 0);

        juego.jugarCarta(cambio);
        juego.cambiarColor("Azul");
        assertTrue(juego.hayUnoPendiente());

        juego.decirUno(); // puede decirlo después de elegir el color
        juego.robarCarta();
        assertFalse(juego.huboPenalizacionUno());
        assertEquals(1, jugador(juego, 0).cantidadCartas());
    }

    // ---------- rondas y partida ----------

    @Test
    void ganarUnaRondaSumaLosPuntosDeLasManosRivales() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        Carta ultima = c("Rojo", "7");
        darMano(jugador(juego, 0), ultima);
        darMano(jugador(juego, 1), c("Rojo", Carta.SALTA), c("Azul", Carta.REVERSA), c("Verde", "5"));
        juego.prepararMesaParaPrueba(c("Rojo", "5"), "Rojo", 0);

        juego.jugarCarta(ultima);

        assertTrue(juego.hayGanador());
        assertEquals("Carlos", juego.obtenerGanador().getNombre());
        assertEquals(45, juego.obtenerPuntosGanadorRonda());
        assertEquals(45, jugador(juego, 0).getPuntos());
        assertNull(juego.obtenerGanadorPartida());
    }

    @Test
    void ganarConUnComodinNoDejaUnColorPendiente() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        Carta ultima = c("Negro", Carta.MAS_CUATRO);
        darMano(jugador(juego, 0), ultima);
        juego.prepararMesaParaPrueba(c("Rojo", "5"), "Rojo", 0);

        juego.jugarCarta(ultima);

        assertTrue(juego.hayGanador());
        assertFalse(juego.hayColorPendiente());
    }

    @Test
    void noDebePermitirJugarNiRobarCuandoLaRondaTermino() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        Carta ultima = c("Rojo", "7");
        darMano(jugador(juego, 0), ultima);
        Carta otra = c("Rojo", "8");
        darMano(jugador(juego, 1), otra, c("Verde", "1"));
        juego.prepararMesaParaPrueba(c("Rojo", "5"), "Rojo", 0);

        juego.jugarCarta(ultima);

        assertThrows(IllegalStateException.class, () -> juego.jugarCarta(otra));
        assertThrows(IllegalStateException.class, juego::robarCarta);
        assertThrows(IllegalStateException.class, juego::decirUno);
    }

    @Test
    void debePermitirReiniciarUnaNuevaRonda() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        Carta ultima = c("Rojo", "7");
        darMano(jugador(juego, 0), ultima);
        juego.prepararMesaParaPrueba(c("Rojo", "5"), "Rojo", 0);
        juego.jugarCarta(ultima);

        int ronda = juego.obtenerNumeroRonda();
        int puntosCarlos = jugador(juego, 0).getPuntos();
        juego.nuevaRonda();

        assertEquals(ronda + 1, juego.obtenerNumeroRonda());
        assertEquals(7, jugador(juego, 0).cantidadCartas());
        assertEquals(7, jugador(juego, 1).cantidadCartas());
        assertFalse(juego.hayGanador());
        assertEquals(puntosCarlos, jugador(juego, 0).getPuntos(), "Los puntos acumulados se conservan");
    }

    @Test
    void noDebePermitirReiniciarUnaPartidaActiva() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        assertThrows(IllegalStateException.class, juego::iniciarJuego);
    }

    @Test
    void noDebePermitirNuevaRondaMientrasLaActualEstaActiva() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        assertThrows(IllegalStateException.class, juego::nuevaRonda);
    }

    @Test
    void alLlegarA250PuntosTerminaLaPartidaYSePuedeIniciarOtra() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        Carta ultima = c("Rojo", "7");
        darMano(jugador(juego, 0), ultima);
        darMano(jugador(juego, 1),
                c("Negro", "+4"), c("Negro", "+4"), c("Negro", "+4"),
                c("Negro", "+4"), c("Negro", "+4")); // 5 x 50 = 250
        juego.prepararMesaParaPrueba(c("Rojo", "5"), "Rojo", 0);

        juego.jugarCarta(ultima);

        assertNotNull(juego.obtenerGanadorPartida());
        assertEquals("Carlos", juego.obtenerGanadorPartida().getNombre());
        assertThrows(IllegalStateException.class, juego::nuevaRonda);

        // Se puede empezar una partida nueva: puntos y ronda se reinician.
        juego.iniciarJuego();
        assertNull(juego.obtenerGanadorPartida());
        assertEquals(0, jugador(juego, 0).getPuntos());
        assertEquals(1, juego.obtenerNumeroRonda());
        assertEquals(7, jugador(juego, 0).cantidadCartas());
    }

    // ---------- mazo agotado ----------

    @Test
    void cuandoSeAcabaElMazoSeReciclaLaPilaSinPerderLaCartaSuperior() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        while (juego.cartasEnMazo() > 0) {
            juego.robarCarta();
        }
        assertEquals(0, juego.cartasEnMazo());

        List<Carta> pilaPrueba = new ArrayList<>();
        for (int i = 1; i <= 5; i++) pilaPrueba.add(c("Rojo", String.valueOf(i)));
        Carta superior = pilaPrueba.get(4);
        juego.prepararMesaParaPrueba(pilaPrueba, "Rojo", 0);

        Carta robada = juego.robarCarta();

        assertNotNull(robada);
        assertSame(superior, juego.obtenerCartaSuperior());
        assertEquals(1, juego.cartasEnPila());
        assertEquals(3, juego.cartasEnMazo()); // 4 recicladas - 1 robada
    }

    @Test
    void siNoQuedanCartasRobarSoloPasaElTurno() {
        JuegoUno juego = juegoConJugadores("Carlos", "Ana");
        while (juego.cartasEnMazo() > 0) {
            juego.robarCarta();
        }
        String antes = juego.obtenerJugadorActual().getNombre();

        assertDoesNotThrow(() -> assertNull(juego.robarCarta()));
        assertNotEquals(antes, juego.obtenerJugadorActual().getNombre());
    }

    // ---------- partidas simuladas ----------

    /** Juega partidas completas con jugadas legales al azar y verifica los invariantes. */
    @Test
    void partidasSimuladasNoRompenReglasNiPierdenCartas() {
        java.util.Random azar = new java.util.Random(12345);

        for (int partida = 0; partida < 150; partida++) {
            int n = 2 + azar.nextInt(9);
            JuegoUno juego = new JuegoUno();
            for (int i = 0; i < n; i++) juego.agregarJugador("J" + i);
            juego.iniciarJuego();

            for (int paso = 0; paso < 20000 && juego.obtenerGanadorPartida() == null; paso++) {
                if (juego.hayGanador()) {
                    juego.nuevaRonda();
                    continue;
                }
                simularJugada(juego, azar);
                assertEquals(108, totalCartas(juego), "Se perdieron o duplicaron cartas");
            }
        }
    }

    private int totalCartas(JuegoUno juego) {
        int total = juego.cartasEnMazo() + juego.cartasEnPila();
        for (Jugador j : juego.getJugadores()) total += j.cantidadCartas();
        return total;
    }

    private void simularJugada(JuegoUno juego, java.util.Random azar) {
        // Casi siempre se dice UNO a tiempo; a veces se "olvida" para probar la penalización.
        if (juego.hayUnoPendiente() && azar.nextInt(10) < 7) {
            juego.decirUno();
        }

        if (juego.hayColorPendiente()) {
            String[] colores = {"Rojo", "Azul", "Verde", "Amarillo"};
            juego.cambiarColor(colores[azar.nextInt(4)]);
            return;
        }

        Jugador actual = juego.obtenerJugadorActual();
        List<Carta> mano = new ArrayList<>(actual.getMano());
        java.util.Collections.shuffle(mano, azar);

        for (Carta carta : mano) {
            try {
                juego.jugarCarta(carta);
                return;
            } catch (IllegalArgumentException jugadaInvalida) {
                // probar con otra carta
            }
        }
        juego.robarCarta();
    }
}
