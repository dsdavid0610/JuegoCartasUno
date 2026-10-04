package edu.unilibre.uno;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Jugador {
    private final String nombre;
    private final List<Carta> mano;
    private int puntos;

    public Jugador(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del jugador no puede estar vacío.");
        }
        this.nombre = nombre.trim();
        this.mano = new ArrayList<>();
        this.puntos = 0;
    }

    public String getNombre() { return nombre; }

    /** Vista de solo lectura de la mano; para modificarla usa los métodos de Jugador. */
    public List<Carta> getMano() { return Collections.unmodifiableList(mano); }

    public int getPuntos() { return puntos; }

    public void agregarCarta(Carta carta) {
        if (carta != null) mano.add(carta);
    }

    public void recibirCarta(Carta carta) { agregarCarta(carta); }

    public void eliminarCarta(Carta carta) {
        if (carta != null) mano.remove(carta);
    }

    public boolean tieneCarta(Carta carta) { return mano.contains(carta); }

    public int cantidadCartas() { return mano.size(); }

    public boolean gano() { return mano.isEmpty(); }

    public void limpiarMano() { mano.clear(); }

    public void sumarPuntos(int cantidad) {
        if (cantidad > 0) puntos += cantidad;
    }

    public void reiniciarPuntos() { puntos = 0; }

    public boolean puedeJugar(Carta cartaSuperior) {
        return puedeJugar(cartaSuperior, null);
    }

    public boolean puedeJugar(Carta cartaSuperior, String colorActual) {
        if (cartaSuperior == null) return true;
        for (Carta carta : mano) {
            if (carta != null && carta.sePuedeJugarSobre(cartaSuperior, colorActual)) {
                return true;
            }
        }
        return false;
    }

    public boolean tieneCartaDeColor(String color) {
        if (color == null || Carta.NEGRO.equals(color)) return false;
        for (Carta carta : mano) {
            if (carta != null && color.equals(carta.getColor())) return true;
        }
        return false;
    }

    public int calcularPuntosMano() {
        int total = 0;
        for (Carta carta : mano) {
            if (carta != null) total += carta.puntos();
        }
        return total;
    }

    @Override
    public String toString() {
        return nombre + " tiene " + mano.size() + " cartas - " + puntos + " puntos";
    }
}
