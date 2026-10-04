package edu.unilibre.uno;

public class Carta {
    public static final String NEGRO = "Negro";
    public static final String SALTA = "Salta";
    public static final String REVERSA = "Reversa";
    public static final String MAS_DOS = "+2";
    public static final String MAS_CUATRO = "+4";
    public static final String CAMBIO_COLOR = "Cambio De Color";

    private final String color;
    private final String valor;

    public Carta(String color, String valor) {
        this.color = color;
        this.valor = valor;
    }

    public String getColor() { return color; }
    public String getValor() { return valor; }

    public boolean esSalta() { return SALTA.equals(valor); }
    public boolean esReversa() { return REVERSA.equals(valor); }
    public boolean esMasDos() { return MAS_DOS.equals(valor); }
    public boolean esMasCuatro() { return MAS_CUATRO.equals(valor); }
    public boolean esCambioColor() { return CAMBIO_COLOR.equals(valor); }

    /** Los comodines (+4 y cambio de color) no tienen color propio: siempre son NEGRO. */
    public boolean esComodin() {
        return NEGRO.equals(color) || esMasCuatro() || esCambioColor();
    }

    public boolean esEspecial() {
        return esSalta() || esReversa() || esMasDos() || esMasCuatro() || esCambioColor();
    }

    /** Puntos que vale la carta al final de una ronda. */
    public int puntos() {
        if (esMasCuatro() || esCambioColor()) return 50;
        if (esMasDos() || esSalta() || esReversa()) return 20;
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean sePuedeJugarSobre(Carta cartaSuperior) {
        return sePuedeJugarSobre(cartaSuperior, null);
    }

    /**
     * Única fuente de verdad de la regla de compatibilidad entre cartas.
     * Usa el color efectivo de la mesa, de modo que un comodín siga siendo NEGRO
     * internamente aunque se haya elegido un color para la siguiente jugada.
     */
    public boolean sePuedeJugarSobre(Carta cartaSuperior, String colorActual) {
        if (cartaSuperior == null) return true;
        if (esComodin()) return true;
        String colorReferencia = colorActual == null ? cartaSuperior.getColor() : colorActual;
        return color.equals(colorReferencia) || valor.equals(cartaSuperior.getValor());
    }

    @Override
    public String toString() { return color + " " + valor; }
}
