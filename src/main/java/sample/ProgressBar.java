package sample;

import javax.swing.*;
import java.awt.*;

public class ProgressBar {

    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JPanel panel;

    public ProgressBar(){

        panel = new JPanel();
        //panel.setBorder(BorderFactory.createEmptyBorder(50, 10, 50, 10));

        // Создаем метку для отображения статусов
        statusLabel = new JLabel("Ожидание данных...");
        panel.add(statusLabel, BorderLayout.SOUTH);

        // Создаем полосу прогресса для отображения загрузки
        progressBar = new JProgressBar();
        panel.add(progressBar, BorderLayout.NORTH);

    }

    public void setStatus(String status, int progress) {
        statusLabel.setText(status);
        progressBar.setValue(progress);
    }

    public JPanel getPanel(){
        return panel;
    }

}
