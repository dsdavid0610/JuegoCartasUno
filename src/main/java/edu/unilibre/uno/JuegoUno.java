package edu.unilibre.uno;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JuegoUno {
    public static final int MIN_JUGADORES = 2;
    public static final int MAX_JUGADORES = 10;
    public static final int CARTAS_INICIALES = 7;
    public static final int PUNTOS_PARA_GANAR = 250;
    public static final int CARTAS_PENALIZACION_UNO = 2;

    private final Mazo mazo;
    private final List<Jugador> jugadores;
    private final List<Jugador> jugadoresSoloLectura;
    private final List<Carta> pila;

    private int turno;
    private boolean sentidoHorario;
    /** true mientras hay una partida en curso (hasta que alguien llega a los puntos meta). */
    private boolean rondaIniciada;
    /** true cuando alguien se quedó sin cartas en la ronda actual. */
    private boolean rondaTerminada;

    /** Alguien se quedó con 1 carta y todavía no ha dicho UNO. */
    private boolean unoPendiente;
    private Jugador jugadorQueDebeDecirUno;
    /** La última acción terminó penalizando a alguien por no decir UNO. */
    private boolean penalizadoPorUno;
    private Jugador jugadorPenalizadoUno;

    private int puntosGanador;
    private int numeroRonda;
    private Jugador ganadorPartida;
    private String colorActual;
    private boolean colorPendiente;
    private int penalizacionAcumulada;

    public JuegoUno() {
        mazo = new Mazo();
        jugadores = new ArrayList<>();
        jugadoresSoloLectura = Collections.unmodifiableList(jugadores);
        pila = new ArrayList<>();
        numeroRonda = 1;
        sentidoHorario = true;
    }

    // ------------------------------------------------------------------
    // Jugadores
    // ------------------------------------------------------------------

    public void agregarJugador(String nombre) {
        if (rondaIniciada) {
            throw new IllegalStateException("No puedes agregar jugadores con el juego iniciado.");
        }
        if (jugadores.size() >= MAX_JUGADORES) {
            throw new IllegalArgumentException("No pueden existir más de " + MAX_JUGADORES + " jugadores.");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del jugador no puede estar vacío.");
        }
        jugadores.add(new Jugador(nombre));
    }

    public void limpiarJugadores() {
        if (rondaIniciada) {
            throw new IllegalStateException("No puedes limpiar los jugadores durante una partida.");
        }
        jugadores.clear();
    }

    /** Lista de solo lectura. */
    public List<Jugador> getJugadores() { return jugadoresSoloLectura; }

    public int cantidadJugadores() { return jugadores.size(); }

    // ------------------------------------------------------------------
    // Inicio de partida y de rondas
    // ------------------------------------------------------------------

    public void iniciarJuego() {
        if (rondaIniciada) {
            throw new IllegalStateException("La partida ya está en curso.");
        }
        if (jugadores.size() < MIN_JUGADORES) {
            throw new IllegalStateException("Se necesitan mínimo " + MIN_JUGADORES + " jugadores.");
        }
        if (jugadores.size() > MAX_JUGADORES) {
            throw new IllegalStateException("Máximo " + MAX_JUGADORES + " jugadores.");
        }

        for (Jugador jugador : jugadores) {
            jugador.reiniciarPuntos();
        }
        numeroRonda = 1;
        ganadorPartida = null;
        rondaIniciada = true;
        prepararRonda();
    }

    public void nuevaRonda() {
        if (ganadorPartida != null) {
            throw new IllegalStateException("La partida ya terminó.");
        }
        if (!rondaTerminada) {
            throw new IllegalStateException("La ronda actual todavía no ha terminado.");
        }
        numeroRonda++;
        prepararRonda();
    }

    private void prepararRonda() {
        mazo.reiniciar();
        pila.clear();
        for (Jugador jugador : jugadores) {
            jugador.limpiarMano();
        }

        turno = 0;
        sentidoHorario = true;
        rondaTerminada = false;
        unoPendiente = false;
        jugadorQueDebeDecirUno = null;
        penalizadoPorUno = false;
        jugadorPenalizadoUno = null;
        puntosGanador = 0;
        colorActual = null;
        colorPendiente = false;
        penalizacionAcumulada = 0;

        repartirCartasIniciales();
        colocarPrimeraCarta();
        aplicarReglaCartaInicial();
    }

    private void repartirCartasIniciales() {
        for (int i = 0; i < CARTAS_INICIALES; i++) {
            for (Jugador jugador : jugadores) {
                jugador.agregarCarta(mazo.robar());
            }
        }
    }

    private void colocarPrimeraCarta() {
        // La carta inicial no puede ser +2, +4 ni cambio de color.
        // Las rechazadas no se pierden: vuelven al mazo.
        List<Carta> rechazadas = new ArrayList<>();
        Carta primera;
        while (true) {
            primera = mazo.robar();
            if (primera == null) {
                throw new IllegalStateException("No hay cartas disponibles.");
            }
            if (!primera.esMasDos() && !primera.esMasCuatro() && !primera.esCambioColor()) {
                break;
            }
            rechazadas.add(primera);
        }
        mazo.reponer(rechazadas);

        pila.add(primera);
        colorActual = primera.getColor();
        colorPendiente = false;
    }

    private void aplicarReglaCartaInicial() {
        Carta carta = obtenerCartaSuperior();
        if (carta == null) return;

        if (carta.esReversa()) {
            sentidoHorario = !sentidoHorario;
        } else if (carta.esSalta()) {
            cambiarTurno();
        }
    }

    // ------------------------------------------------------------------
    // Jugadas
    // ------------------------------------------------------------------

    private void validarRondaActiva() {
        if (rondaTerminada) {
            throw new IllegalStateException("La ronda ya terminó.");
        }
        if (!rondaIniciada) {
            throw new IllegalStateException("Primero debes iniciar el juego.");
        }
    }

    public void jugarCarta(Carta carta) {
        validarRondaActiva();
        if (carta == null) {
            throw new IllegalArgumentException("La carta no puede ser nula.");
        }
        Jugador jugador = obtenerJugadorActual();
        if (jugador == null) {
            throw new IllegalStateException("No hay jugador actual.");
        }
        if (!jugador.tieneCarta(carta)) {
            throw new IllegalArgumentException("Esa carta no pertenece a tu mano.");
        }
        if (colorPendiente) {
            throw new IllegalArgumentException("Debes seleccionar un color primero.");
        }

        if (penalizacionAcumulada > 0) {
            if (!carta.esMasDos() && !carta.esMasCuatro()) {
                throw new IllegalArgumentException(
                        "Debes jugar un +2 o un +4 para contrarrestar "
                                + penalizacionAcumulada + " cartas.");
            }
        } else {
            if (!carta.sePuedeJugarSobre(obtenerCartaSuperior(), colorActual)) {
                throw new IllegalArgumentException("No puedes jugar esa carta.");
            }
            if (carta.esMasCuatro() && jugador.tieneCartaDeColor(colorActual)) {
                throw new IllegalArgumentException(
                        "No puedes usar +4 porque tienes una carta del color actual.");
            }
        }

        // A partir de aquí la jugada es válida.
        // Si el jugador anterior se quedó con 1 carta y no dijo UNO, se le penaliza ahora
        // (salvo que quien juega sea él mismo y esté sacando su última carta).
        resolverUnoPendiente(jugador, jugador.cantidadCartas() == 1);
        jugador.eliminarCarta(carta);
        pila.add(carta);

        if (!carta.esComodin()) {
            colorActual = carta.getColor();
        }
        if (carta.esMasDos()) {
            penalizacionAcumulada += 2;
        } else if (carta.esMasCuatro()) {
            penalizacionAcumulada += 4;
        }

        if (jugador.cantidadCartas() == 0) {
            finalizarRonda(jugador);
            return;
        }

        if (carta.esComodin()) {
            colorPendiente = true;
        }

        // Regla de UNO: al jugar la penúltima carta te queda 1 y debes decir UNO
        // antes de que el siguiente jugador haga su jugada.
        if (jugador.cantidadCartas() == 1) {
            unoPendiente = true;
            jugadorQueDebeDecirUno = jugador;
        }

        aplicarReglaEspecial(carta);
    }

    private void aplicarReglaEspecial(Carta carta) {
        if (carta.esSalta()) {
            cambiarTurno();
            cambiarTurno();
        } else if (carta.esReversa()) {
            sentidoHorario = !sentidoHorario;
            cambiarTurno();
            if (jugadores.size() == 2) {
                cambiarTurno(); // con 2 jugadores, Reversa funciona como Salta
            }
        } else if (carta.esComodin()) {
            // El turno avanza cuando se elige el color (cambiarColor).
        } else {
            cambiarTurno(); // números y +2
        }
    }

    private void resolverUnoPendiente(Jugador actor, boolean actorSaleDeLaRonda) {
        penalizadoPorUno = false;
        jugadorPenalizadoUno = null;
        if (!unoPendiente) return;

        Jugador deudor = jugadorQueDebeDecirUno;
        unoPendiente = false;
        jugadorQueDebeDecirUno = null;

        if (deudor == null || (deudor == actor && actorSaleDeLaRonda)) return;

        for (int i = 0; i < CARTAS_PENALIZACION_UNO; i++) {
            deudor.agregarCarta(robarDelMazo());
        }
        penalizadoPorUno = true;
        jugadorPenalizadoUno = deudor;
    }

    private void cambiarTurno() {
        if (jugadores.isEmpty()) return;
        if (sentidoHorario) {
            turno = (turno + 1) % jugadores.size();
        } else {
            turno = (turno - 1 + jugadores.size()) % jugadores.size();
        }
    }

    public Carta robarCarta() {
        validarRondaActiva();
        if (colorPendiente) {
            throw new IllegalStateException("Debes seleccionar un color primero.");
        }
        Jugador jugador = obtenerJugadorActual();
        if (jugador == null) {
            throw new IllegalStateException("No hay jugador actual.");
        }

        resolverUnoPendiente(jugador, false);

        if (penalizacionAcumulada > 0) {
            for (int i = 0; i < penalizacionAcumulada; i++) {
                jugador.agregarCarta(robarDelMazo());
            }
            penalizacionAcumulada = 0;
            cambiarTurno();
            return null;
        }

        Carta carta = robarDelMazo();
        if (carta != null) {
            jugador.agregarCarta(carta);
        }
        // Si no queda ninguna carta (ni en el mazo ni que reciclar), solo se pasa el turno.
        cambiarTurno();
        return carta;
    }

    /** Roba del mazo; si está vacío, baraja la pila de descarte (menos la carta superior) y la reutiliza. */
    private Carta robarDelMazo() {
        if (mazo.cantidadCartas() == 0) {
            reciclarPila();
        }
        return mazo.robar();
    }

    private void reciclarPila() {
        if (pila.size() <= 1) return;
        Carta superior = pila.remove(pila.size() - 1);
        mazo.reponer(new ArrayList<>(pila));
        pila.clear();
        pila.add(superior);
    }

    /**
     * Dice UNO. Es válido cuando algún jugador se quedó con 1 carta y todavía
     * no lo ha dicho; debe hacerse antes de que el siguiente jugador juegue o robe.
     */
    public void decirUno() {
        validarRondaActiva();
        if (!unoPendiente) {
            throw new IllegalStateException("Nadie tiene que decir UNO en este momento.");
        }
        unoPendiente = false;
        jugadorQueDebeDecirUno = null;
    }

    public void cambiarColor(String color) {
        if (!colorPendiente) {
            throw new IllegalStateException("No hay ningún cambio de color pendiente.");
        }
        if (color == null) {
            throw new IllegalArgumentException("Debes seleccionar un color.");
        }

        switch (color.trim().toLowerCase()) {
            case "rojo":     colorActual = "Rojo";     break;
            case "azul":     colorActual = "Azul";     break;
            case "verde":    colorActual = "Verde";    break;
            case "amarillo": colorActual = "Amarillo"; break;
            default:
                throw new IllegalArgumentException("Color no válido.");
        }

        colorPendiente = false;
        cambiarTurno();
    }

    // ------------------------------------------------------------------
    // Fin de ronda y puntaje
    // ------------------------------------------------------------------

    private void finalizarRonda(Jugador ganador) {
        rondaTerminada = true;
        puntosGanador = calcularPuntosRonda(ganador);
        ganador.sumarPuntos(puntosGanador);

        if (ganador.getPuntos() >= PUNTOS_PARA_GANAR) {
            ganadorPartida = ganador;
            rondaIniciada = false; // la partida terminó: se puede iniciar otra
        }
    }

    private int calcularPuntosRonda(Jugador ganador) {
        int puntos = 0;
        for (Jugador jugador : jugadores) {
            if (jugador != ganador) {
                puntos += jugador.calcularPuntosMano();
            }
        }
        return puntos;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public Jugador obtenerJugadorActual() {
        if (jugadores.isEmpty()) return null;
        if (turno < 0 || turno >= jugadores.size()) turno = 0;
        return jugadores.get(turno);
    }

    public Carta obtenerCartaSuperior() {
        return pila.isEmpty() ? null : pila.get(pila.size() - 1);
    }

    public String obtenerColorActual() { return colorActual; }

    public boolean hayColorPendiente() { return colorPendiente; }

    /** Hay un jugador con 1 carta que todavía no ha dicho UNO. */
    public boolean hayUnoPendiente() {
        return unoPendiente && !rondaTerminada;
    }

    public Jugador obtenerJugadorQueDebeDecirUno() {
        return hayUnoPendiente() ? jugadorQueDebeDecirUno : null;
    }

    /** true si la última acción terminó penalizando a alguien por no decir UNO. */
    public boolean huboPenalizacionUno() { return penalizadoPorUno; }

    public Jugador obtenerJugadorPenalizadoUno() { return jugadorPenalizadoUno; }

    public boolean hayGanador() {
        return obtenerGanador() != null;
    }

    public Jugador obtenerGanador() {
        for (Jugador jugador : jugadores) {
            if (jugador.cantidadCartas() == 0) return jugador;
        }
        return null;
    }

    public Jugador obtenerGanadorPartida() { return ganadorPartida; }

    public int obtenerPuntosGanadorRonda() { return puntosGanador; }

    public int obtenerNumeroRonda() { return numeroRonda; }

    public int cartasEnMazo() { return mazo.cantidadCartas(); }

    public int cartasEnPila() { return pila.size(); }

    public int obtenerPenalizacionAcumulada() { return penalizacionAcumulada; }

    public boolean puedeContrarrestarPenalizacion() {
        if (penalizacionAcumulada <= 0) return true;
        Jugador jugador = obtenerJugadorActual();
        if (jugador == null) return false;
        for (Carta carta : jugador.getMano()) {
            if (carta.esMasDos() || carta.esMasCuatro()) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Soporte para pruebas (visible solo dentro del paquete)
    // ------------------------------------------------------------------

    void prepararMesaParaPrueba(Carta superior, String color, int turnoActual) {
        prepararMesaParaPrueba(List.of(superior), color, turnoActual);
    }

    /** La última carta de la lista queda como carta superior de la pila. */
    void prepararMesaParaPrueba(List<Carta> nuevaPila, String color, int turnoActual) {
        pila.clear();
        pila.addAll(nuevaPila);
        colorActual = color;
        turno = turnoActual;
        sentidoHorario = true;
        unoPendiente = false;
        jugadorQueDebeDecirUno = null;
        penalizadoPorUno = false;
        jugadorPenalizadoUno = null;
        penalizacionAcumulada = 0;
        colorPendiente = false;
    }
}
