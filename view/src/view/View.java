package view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final public class View extends JDialog {
    final private static String SAVE = "\u2714";
    final private static String DELETE = "\u2716";    

    private View(Year year) {
        setTitle(year.toString());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        
        HashMap<LocalDate, String> notes = new HashMap();
        
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(getTitle()))) {
            notes.putAll((HashMap) objectInputStream.readObject());
        } catch (Exception ex) { }

        CardLayout cardLayout = new CardLayout();
        JPanel yearPanel = new JPanel(cardLayout);

        for (int week = (year.atDay(1).getDayOfWeek().ordinal() + LocalDate.now().withYear(year.getValue()).getDayOfYear() - 1) / DayOfWeek.values().length, december31 = 53 + year.length() / 366 / year.atDay(year.length()).getDayOfWeek().getValue(); yearPanel.getComponentCount() < december31; week = ++week % december31) {
            JPanel weekPanel = new JPanel(new BorderLayout());
            weekPanel.add(new JLabel("week " + (week + 1 - year.atDay(1).getDayOfWeek().getValue() / DayOfWeek.FRIDAY.getValue()), JLabel.CENTER), BorderLayout.NORTH);

            JPanel monday_Friday = new JPanel(new GridLayout(3, 2));
            JPanel saturday_Sunday = new JPanel(new GridLayout(1, 2));

            for (DayOfWeek day : new DayOfWeek[] {DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.TUESDAY, DayOfWeek.FRIDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY}) {
                JPanel dayPanel = new JPanel(new BorderLayout());
                dayPanel.setBorder(BorderFactory.createMatteBorder(1, day.getValue() / DayOfWeek.THURSDAY.getValue(), 0, 0, Color.darkGray));
                
                LocalDate localDate = year.atDay(1).minusDays(year.atDay(1).getDayOfWeek().ordinal()).plusWeeks(week).plusDays(day.ordinal());

                if (localDate.getYear() == year.getValue()) {
                    JLabel dateLabel = new JLabel(localDate.getDayOfMonth() + "\u2010" + localDate.getMonthValue(), JLabel.CENTER);
                    JButton ioButton = new JButton(SAVE);
                    JTextArea textArea = new JTextArea(notes.getOrDefault(localDate, ""), 4, 9);
                    JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                    
                    if (localDate.equals(LocalDate.now())) {
                        dateLabel.setForeground(Color.red);
                    }
                    dateLabel.addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusGained(FocusEvent e) {
                            ioButton.setText(DELETE);
                            ioButton.setVisible(true);
                        }
                        @Override
                        public void focusLost(FocusEvent e) {
                            ioButton.setVisible(false);
                            ioButton.setText(SAVE);
                        }
                    });
                    dateLabel.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            if (ioButton.isVisible()) {
                                requestFocus();
                            } else if (notes.containsKey(localDate)) {
                                dateLabel.requestFocus();
                            }
                        }
                    });
                    dateLabel.setLayout(new FlowLayout(FlowLayout.RIGHT, 1, 0));
                    dateLabel.add(ioButton); 

                    ioButton.setContentAreaFilled(false);
                    ioButton.setBorder(null);
                    ioButton.setFocusable(false);
                    ioButton.setFont(ioButton.getFont().deriveFont(Font.PLAIN));
                    ioButton.setVisible(false);
                    ioButton.addActionListener(e -> {
                        if (e.getActionCommand().equals(DELETE)) {
                            notes.remove(localDate);

                            textArea.setText("");
                        } else if (!textArea.getText().trim().isEmpty()) {
                            notes.put(localDate, textArea.getText());
                        }

                        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(getTitle()))) {
                            objectOutputStream.writeObject(notes);
                        } catch (Exception ex) { }

                        requestFocus();
                    });
                    
                    textArea.setLineWrap(true);
                    textArea.setWrapStyleWord(true);
                    textArea.addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusGained(FocusEvent e) {
                            ioButton.setVisible(true);
                        }
                        @Override
                        public void focusLost(FocusEvent e) {
                            ioButton.setVisible(false);

                            try {
                                textArea.setText(notes.get(localDate));
                                textArea.setCaretPosition(0);
                            } catch (Exception ex) { 
                                textArea.setText("");
                            }
                        }
                    });

                    scrollPane.setBorder(null);
                    scrollPane.setViewportView(textArea);

                    dayPanel.add(dateLabel, BorderLayout.NORTH);
                    dayPanel.add(scrollPane, BorderLayout.CENTER);
                }

                new JPanel[] {monday_Friday, saturday_Sunday}[day.getValue() / DayOfWeek.SATURDAY.getValue()].add(dayPanel);
            }

            monday_Friday.add(saturday_Sunday);
            weekPanel.add(monday_Friday, BorderLayout.CENTER);
            yearPanel.add(weekPanel);
        }

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    cardLayout.previous(yearPanel);
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    cardLayout.next(yearPanel);
                }
            }
        });

        setContentPane(yearPanel);
        pack();
        setVisible(true);
        setLocationRelativeTo(null);

        requestFocus();
    }

    public static void main(String[] args) {
        Year year = Year.now();
            
        try {
            year = Year.of(Integer.parseInt(args[0]));
        } catch (Exception ex) { }

        new View(year);
    }
    
}
