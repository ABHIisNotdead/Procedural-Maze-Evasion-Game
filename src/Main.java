import ui.GameWindow;
import javax.swing.JFrame;

public class Main {
    public static void main(String[] arg) {
        JFrame frame = new JFrame("Procedural maze Evasion");
        GameWindow gameWindow = new GameWindow();

        frame.add(gameWindow);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

    }
}
