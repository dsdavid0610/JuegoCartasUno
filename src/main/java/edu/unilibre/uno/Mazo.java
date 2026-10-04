package edu.unilibre.uno;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Mazo {
    private final List<Carta> cartas;

    public Mazo() {
        cartas = new ArrayList<>();
        crearCartas();
        barajar();
    }

    private void crearCartas() {
        String[] colores = {"Rojo", "Azul", "Verde", "Amarillo"};
        for (String color : colores) {
            cartas.add(new Carta(color, "0"));
            for (int i = 1; i <= 9; i++) {
                cartas.add(new Carta(color, String.valueOf(i)));
                cartas.add(new Carta(color, String.valueOf(i)));
            }
            for (int i = 0; i < 2; i++) {
                cartas.add(new Carta(color, Carta.SALTA));
                cartas.add(new Carta(color, Carta.REVERSA));
                cartas.add(new Carta(color, Carta.MAS_DOS));
            }
        }
        for (int i = 0; i < 4; i++) {
            cartas.add(new Carta(Carta.NEGRO, Carta.CAMBIO_COLOR));
            cartas.add(new Carta(Carta.NEGRO, Carta.MAS_CUATRO));
        }
    }

    public void reiniciar() {
        cartas.clear();
        crearCartas();
        barajar();
    }

    public void barajar() { Collections.shuffle(cartas); }

    public Carta robar() { return cartas.isEmpty() ? null : cartas.remove(cartas.size() - 1); }

    /** Agrega cartas al mazo (por ejemplo, la pila de descarte) y lo vuelve a barajar. */
    public void reponer(List<Carta> nuevas) {
        if (nuevas == null || nuevas.isEmpty()) return;
        cartas.addAll(nuevas);
        barajar();
    }

    public int cantidadCartas() { return cartas.size(); }
}
