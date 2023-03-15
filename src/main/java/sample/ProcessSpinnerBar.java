package sample;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class ProcessSpinnerBar {

    private JPanel panel;
    private static JLabel spinnerLabel;

    private static int spinnerAngle = 0;
    private static Timer spinnerTimer;

    public ProcessSpinnerBar(){
        // Создаем панель для отображения элементов логотипа
        panel = new JPanel(new BorderLayout());
        //panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Создаем метку для отображения крутящегося спиннера
        spinnerLabel = new JLabel(new ImageIcon("spinner.gif"));
        panel.add(spinnerLabel, BorderLayout.CENTER);
    }

    public void rotateStart(){

        spinnerLabel.setVisible(true);
        // Создаем таймер для поворота спиннера
        spinnerTimer = new Timer(25, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                spinnerAngle += 5;
                spinnerLabel.setIcon(rotateImageIcon(new ImageIcon("spinner.png"), spinnerAngle));
            }
        });
        spinnerTimer.start();

    }

    public void rotateStop(){
        spinnerTimer.stop();
        spinnerLabel.setVisible(false);
    }

    private static ImageIcon rotateImageIcon(ImageIcon icon, int angle) {
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();

        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bi.createGraphics();
        g2d.rotate(Math.toRadians(angle), w/2, h/2);
        icon.paintIcon(null, g2d, 0, 0);
        g2d.dispose();

        return new ImageIcon(bi);
    }

    public JPanel getPanel(){
        return panel;
    }

}
