package edu.unilibre.uno;

import javax.swing.*;
import java.awt.*;

public class VentanaUno extends JFrame {

    private final JuegoUno juego = new JuegoUno();
    private final JTextField[] campos = new JTextField[10];
    private final JLabel[] etiquetas = new JLabel[10];
    private final JPanel panelMano = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
    private final JTextArea txtArea = new JTextArea();
    private final JLabel lblMesa = new JLabel("Mesa: -", SwingConstants.CENTER);
    private final JLabel lblTurno = new JLabel("Turno: -", SwingConstants.CENTER);
    private final JLabel lblMazo = new JLabel("Mazo: -", SwingConstants.CENTER);

    private JPanel raiz, mesa, centro;
    private JButton btnAgregarJugador, btnIniciar, btnRobar, btnUno, btnNuevaRonda;
    private int cantidadJugadoresVisibles = 2;

    public VentanaUno() {
        setTitle("Juego UNO");
        setSize(1100, 760);
        setMinimumSize(new Dimension(900, 650));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponentes();
    }

    private void initComponentes() {
        raiz = new JPanel(new BorderLayout(12, 12));
        raiz.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        raiz.setBackground(new Color(20, 95, 155));

        crearRegistroJugadores();
        crearCentro();
        crearMano();
        crearEventos();

        add(raiz);
        mostrarEstado();
    }

    private void crearRegistroJugadores() {
        JPanel registro = new JPanel(new GridLayout(0, 2, 5, 5));
        registro.setBorder(BorderFactory.createTitledBorder("Jugadores (2 a 10)"));
        registro.setBackground(Color.WHITE);

        for (int i = 0; i < 10; i++) {
            etiquetas[i] = new JLabel("Jugador " + (i + 1) + ":");
            campos[i] = new JTextField();
            registro.add(etiquetas[i]);
            registro.add(campos[i]);
        }

        btnAgregarJugador = new JButton("Añadir jugador");
        btnIniciar = new JButton("Iniciar juego");

        registro.add(new JLabel());
        registro.add(btnAgregarJugador);
        registro.add(new JLabel());
        registro.add(btnIniciar);

        ocultarJugadores();

        JPanel acciones = new JPanel(new GridLayout(0, 1, 6, 6));
        acciones.setBorder(BorderFactory.createTitledBorder("Acciones"));
        acciones.setBackground(Color.WHITE);

        btnRobar = new JButton("Robar carta");
        btnUno = new JButton("¡UNO!");
        btnNuevaRonda = new JButton("Nueva ronda");

        acciones.add(btnRobar);
        acciones.add(btnUno);
        acciones.add(btnNuevaRonda);

        JPanel izquierda = new JPanel(new BorderLayout(8, 8));
        izquierda.setBackground(new Color(20, 95, 155));
        izquierda.add(registro, BorderLayout.NORTH);
        izquierda.add(acciones, BorderLayout.SOUTH);

        raiz.add(izquierda, BorderLayout.WEST);
    }

    private void crearCentro() {
        centro = new JPanel(new BorderLayout(8, 8));
        centro.setOpaque(false);

        mesa = new JPanel(new BorderLayout(8, 8));
        mesa.setBackground(new Color(25, 125, 190));
        mesa.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        lblTurno.setForeground(Color.WHITE);
        lblTurno.setFont(new Font("Arial", Font.BOLD, 18));

        lblMesa.setForeground(Color.WHITE);
        lblMesa.setFont(new Font("Arial", Font.BOLD, 28));

        lblMazo.setForeground(Color.WHITE);

        mesa.add(lblTurno, BorderLayout.NORTH);
        mesa.add(lblMesa, BorderLayout.CENTER);
        mesa.add(lblMazo, BorderLayout.SOUTH);

        centro.add(mesa, BorderLayout.CENTER);
        crearInformacion();
        raiz.add(centro, BorderLayout.CENTER);
    }

    private void crearInformacion() {
        txtArea.setEditable(false);
        txtArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txtArea.setRows(10);

        JScrollPane info = new JScrollPane(txtArea);
        info.setBorder(BorderFactory.createTitledBorder("Estado y puntuación"));
        centro.add(info, BorderLayout.SOUTH);
    }

    private void crearMano() {
        panelMano.setBackground(new Color(18, 85, 135));
        panelMano.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE),
                "Tu mano - selecciona una carta"));

        cambiarColorTituloMano(Color.WHITE);
        raiz.add(panelMano, BorderLayout.SOUTH);
    }

    private void crearEventos() {
        btnAgregarJugador.addActionListener(e -> agregarJugador());
        btnIniciar.addActionListener(e -> iniciarJuego());
        btnUno.addActionListener(e -> decirUno());
        btnNuevaRonda.addActionListener(e -> nuevaRonda());

        btnRobar.addActionListener(e -> {
            try {
                int penalizacion = juego.obtenerPenalizacionAcumulada();

                if (penalizacion > 0) {
                    if (juego.puedeContrarrestarPenalizacion()) {
                        mostrarMensaje(
                                "Todavía puedes contrarrestar la penalización con un +2 o +4.",
                                "Penalización acumulada",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    juego.robarCarta();
                    mostrarMensaje(
                            "Has comido " + penalizacion + " cartas.",
                            "Cartas acumuladas",
                            JOptionPane.INFORMATION_MESSAGE);
                    mostrarEstado();
                    avisarPenalizacionUno();
                    return;
                }

                Carta carta = juego.robarCarta();
                mostrarEstado();
                avisarPenalizacionUno();

                if (carta != null)
                    mostrarMensaje("Robaste: " + carta, "Robo",
                            JOptionPane.INFORMATION_MESSAGE);
                else
                    mostrarMensaje("No quedan cartas para robar. Pierdes el turno.", "Mazo vacío",
                            JOptionPane.INFORMATION_MESSAGE);

                revisarFinRonda();

            } catch (RuntimeException ex) {
                mostrarMensaje(ex.getMessage(), "UNO",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        btnNuevaRonda.setEnabled(false);
    }

    private void ocultarJugadores() {
        for (int i = 2; i < 10; i++) {
            etiquetas[i].setVisible(false);
            campos[i].setVisible(false);
        }
    }

    private void agregarJugador() {
        if (cantidadJugadoresVisibles >= 10) {
            btnAgregarJugador.setEnabled(false);
            return;
        }

        int i = cantidadJugadoresVisibles++;
        etiquetas[i].setVisible(true);
        campos[i].setVisible(true);

        if (cantidadJugadoresVisibles >= 10)
            btnAgregarJugador.setEnabled(false);

        revalidate();
        repaint();
    }

    private void iniciarJuego() {
        try {
            juego.limpiarJugadores();

            for (int i = 0; i < cantidadJugadoresVisibles; i++) {
                String nombre = campos[i].getText().trim();

                if (nombre.isEmpty())
                    throw new IllegalArgumentException(
                            "Todos los jugadores visibles deben tener nombre.");

                juego.agregarJugador(nombre);
            }

            juego.iniciarJuego();
            btnAgregarJugador.setEnabled(false);
            btnIniciar.setEnabled(false);
            btnNuevaRonda.setEnabled(false);
            mostrarEstado();

        } catch (RuntimeException ex) {
            mostrarMensaje(ex.getMessage(), "UNO",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void decirUno() {
        try {
            juego.decirUno();
            mostrarEstado();
        } catch (RuntimeException ex) {
            mostrarMensaje(ex.getMessage(), "UNO",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void jugarCarta(Carta carta) {
        try {
            juego.jugarCarta(carta);

            if (juego.hayColorPendiente())
                seleccionarColor();

            mostrarEstado();

            avisarPenalizacionUno();

            revisarFinRonda();

        } catch (RuntimeException ex) {
            mostrarMensaje(ex.getMessage(), "Movimiento no válido",
                    JOptionPane.WARNING_MESSAGE);
            mostrarEstado();
        }
    }

    private void avisarPenalizacionUno() {
        if (juego.huboPenalizacionUno() && juego.obtenerJugadorPenalizadoUno() != null)
            mostrarMensaje(juego.obtenerJugadorPenalizadoUno().getNombre()
                            + " no dijo UNO a tiempo: roba "
                            + JuegoUno.CARTAS_PENALIZACION_UNO + " cartas.",
                    "Penalización por UNO", JOptionPane.WARNING_MESSAGE);
    }

    private void seleccionarColor() {
        String[] colores = {"Rojo", "Azul", "Verde", "Amarillo"};
        String color;

        do {
            color = (String) JOptionPane.showInputDialog(
                    this,
                    "Selecciona el nuevo color:",
                    "Cambio de color",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    colores,
                    colores[0]);
        } while (color == null && juego.hayColorPendiente());

        if (color != null)
            juego.cambiarColor(color);
    }

    private void revisarFinRonda() {
        if (!juego.hayGanador())
            return;

        Jugador ganador = juego.obtenerGanador();

        String mensaje = "¡Ganó la ronda " + ganador.getNombre()
                + "!\nPuntos de la ronda: "
                + juego.obtenerPuntosGanadorRonda()
                + "\nPuntos acumulados: "
                + ganador.getPuntos();

        if (juego.obtenerGanadorPartida() != null) {
            mensaje += "\n\n¡Ganó la partida al alcanzar 250 puntos!"
                    + "\nPulsa \"Iniciar juego\" para jugar otra partida.";
            btnNuevaRonda.setEnabled(false);
            btnIniciar.setEnabled(true);
            btnAgregarJugador.setEnabled(cantidadJugadoresVisibles < 10);
        } else {
            btnNuevaRonda.setEnabled(true);
        }

        mostrarMensaje(mensaje, "Fin de ronda",
                JOptionPane.INFORMATION_MESSAGE);
        mostrarEstado();
    }

    private void nuevaRonda() {
        try {
            juego.nuevaRonda();
            btnNuevaRonda.setEnabled(false);
            mostrarEstado();
        } catch (RuntimeException ex) {
            mostrarMensaje(ex.getMessage(), "UNO",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void mostrarEstado() {
        Jugador actual = juego.obtenerJugadorActual();
        Carta mesa = juego.obtenerCartaSuperior();

        actualizarTurno(actual);
        actualizarMesa(mesa);
        actualizarMazo();
        actualizarPuntuacion();
        actualizarBotonRobar();
        mostrarMano();
    }

    private void actualizarTurno(Jugador actual) {
        String texto = "Turno: "
                + (actual == null ? "-" : actual.getNombre());

        if (juego.hayUnoPendiente())
            texto += "  |  ¡" + juego.obtenerJugadorQueDebeDecirUno().getNombre()
                    + " tiene 1 carta y debe pulsar UNO!";

        lblTurno.setText(texto);
    }

    private void actualizarMesa(Carta carta) {
        String color = juego.obtenerColorActual();

        if (color == null && carta != null)
            color = carta.getColor();

        lblMesa.setText("Mesa: "
                + (carta == null ? "-" : color + " " + carta.getValor()));

        aplicarColorPantalla(color);
    }

    private void actualizarMazo() {
        lblMazo.setText("Cartas en mazo: "
                + juego.cartasEnMazo()
                + "  |  Ronda: "
                + juego.obtenerNumeroRonda());
    }

    private void actualizarBotonRobar() {
        int penalizacion = juego.obtenerPenalizacionAcumulada();

        if (penalizacion <= 0) {
            btnRobar.setText("Robar carta");
            btnRobar.setEnabled(true);
        } else if (juego.puedeContrarrestarPenalizacion()) {
            btnRobar.setText("Jugar +2 o +4");
            btnRobar.setEnabled(false);
        } else {
            btnRobar.setText("Comer " + penalizacion + " cartas");
            btnRobar.setEnabled(true);
        }
    }

    private void actualizarPuntuacion() {
        StringBuilder sb = new StringBuilder("Puntuación acumulada (meta: 250)\n\n");

        for (Jugador j : juego.getJugadores()) {
            sb.append(j.getNombre())
                    .append(" -> ")
                    .append(j.cantidadCartas())
                    .append(" cartas, ")
                    .append(j.getPuntos())
                    .append(" puntos\n");
        }

        if (juego.hayUnoPendiente()) {
            Jugador j = juego.obtenerJugadorQueDebeDecirUno();

            if (j != null)
                sb.append("\n¡").append(j.getNombre())
                        .append(" tiene 1 carta: debe pulsar UNO antes de que juegue el siguiente!");
        }

        txtArea.setText(sb.toString());
    }

    private void aplicarColorPantalla(String color) {
        Color fondo;

        switch (color == null ? "" : color) {
            case "Rojo":
                fondo = new Color(190, 45, 45);
                break;
            case "Azul":
                fondo = new Color(35, 100, 190);
                break;
            case "Verde":
                fondo = new Color(35, 145, 70);
                break;
            case "Amarillo":
                fondo = new Color(235, 195, 35);
                break;
            default:
                fondo = new Color(20, 95, 155);
        }

        raiz.setBackground(fondo);
        mesa.setBackground(fondo);
        panelMano.setBackground(oscurecerColor(fondo));

        Color texto = "Amarillo".equals(color)
                ? Color.BLACK : Color.WHITE;

        lblMesa.setForeground(texto);
        lblTurno.setForeground(texto);
        lblMazo.setForeground(texto);
        cambiarColorTituloMano(texto);

        raiz.revalidate();
        raiz.repaint();
    }

    private Color oscurecerColor(Color c) {
        return new Color(
                Math.max(0, c.getRed() - 45),
                Math.max(0, c.getGreen() - 45),
                Math.max(0, c.getBlue() - 45));
    }

    private void cambiarColorTituloMano(Color color) {
        if (panelMano.getBorder() instanceof javax.swing.border.TitledBorder) {
            javax.swing.border.TitledBorder b =
                    (javax.swing.border.TitledBorder) panelMano.getBorder();
            b.setTitleColor(color);
        }
    }

    private void mostrarMano() {
        panelMano.removeAll();

        Jugador jugador = juego.obtenerJugadorActual();

        if (jugador != null)
            for (Carta carta : jugador.getMano())
                panelMano.add(crearBotonCarta(carta));

        panelMano.revalidate();
        panelMano.repaint();
    }

    private JButton crearBotonCarta(Carta carta) {
        JButton boton = new JButton(
                "<html><center><b>"
                        + carta.getValor()
                        + "</b><br>"
                        + carta.getColor()
                        + "</center></html>");

        boton.setPreferredSize(new Dimension(90, 110));
        boton.setFont(new Font("Arial", Font.BOLD, 13));
        aplicarColorCarta(boton, carta);
        boton.addActionListener(e -> jugarCarta(carta));

        return boton;
    }

    private void aplicarColorCarta(JButton boton, Carta carta) {
        Color fondo;

        switch (carta.getColor()) {
            case "Rojo":
                fondo = new Color(220, 50, 50);
                break;
            case "Azul":
                fondo = new Color(50, 100, 220);
                break;
            case "Verde":
                fondo = new Color(50, 170, 80);
                break;
            case "Amarillo":
                fondo = new Color(240, 200, 40);
                break;
            default:
                fondo = Color.BLACK;
        }

        boton.setBackground(fondo);
        boton.setForeground("Amarillo".equals(carta.getColor())
                ? Color.BLACK : Color.WHITE);
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setBorderPainted(true);
        boton.setFocusPainted(false);
    }

    private void mostrarMensaje(String mensaje, String titulo, int tipo) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, tipo);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaUno ventana = new VentanaUno();
            ventana.setVisible(true);
        });
    }
}

