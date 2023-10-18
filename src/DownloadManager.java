import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.io.File;

public class DownloadManager extends JFrame {
    private Queue<String> downloadQueue;
    private boolean isDownloading;
    private JTextField urlField;
    private JButton downloadButton;
    private JButton browseButton;
    private DefaultListModel<String> downloadListModel;
    private JList<String> downloadList;
    private Map<String, Color> downloadItemColors;

    public DownloadManager() {
        downloadQueue = new LinkedList<>();
        isDownloading = false;
        downloadItemColors = new HashMap<>();
        initializeComponents();
        createLayout();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Download Manager");
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeComponents() {
        urlField = new JTextField(20);
        downloadButton = new JButton("Download");
        browseButton = new JButton("Browse");
        downloadListModel = new DefaultListModel<>();
        downloadList = new JList<>(downloadListModel);
        downloadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        downloadList.setCellRenderer(new MyListCellRenderer());

        //다운로드 버튼 액션리스너
        downloadButton.addActionListener(e -> {
            String url = urlField.getText();
            addToQueue(url);
            urlField.setText("");
        });

        //브라우저 버튼 액션리스너
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String filePath = selectedFile.getAbsolutePath();
                loadUrlsFromFile(filePath);
            }
        });

        //다운로드 리스트 선택변경 리스너
        downloadList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int selectedIndex = downloadList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    String url = downloadList.getSelectedValue();
                    Color color = downloadItemColors.get(url);
                    downloadList.setSelectionBackground(color);
                }
            }
        });
    }

    private void createLayout() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout());
        inputPanel.add(new JLabel("Enter the URL of the file to download:"));
        inputPanel.add(urlField);
        inputPanel.add(downloadButton);
        inputPanel.add(browseButton);

        JScrollPane listScrollPane = new JScrollPane(downloadList);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(listScrollPane, BorderLayout.CENTER);

        add(panel);
    }

    public void addToQueue(String url) {
        downloadQueue.add(url);
        if (!isDownloading) {
            startDownload();
        }
    }

    private void startDownload() {
        isDownloading = true;
        String url = downloadQueue.poll();
        DownloadTask downloadTask = new DownloadTask(url, this);
        Thread thread = new Thread(downloadTask);
        thread.start();

        addToDownloadList(url);
    }

    public void notifyDownloadComplete(String url, boolean success) {
        isDownloading = false;
        if (!downloadQueue.isEmpty()) {
            startDownload();
        }

        if (success) {
            showDownloadCompleteMessage(url);
            highlightDownloadItem(url, Color.GREEN);
        } else {
            showErrorMessage("An error occurred during download.");
            highlightDownloadItem(url, Color.PINK);
        }
    }

    private void showDownloadCompleteMessage(String url) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, "Download complete: " + url, "Download Complete", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    public void showErrorMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    public void addToDownloadList(String url) {
        SwingUtilities.invokeLater(() -> {
            downloadListModel.addElement(url);
        });
    }

    private void loadUrlsFromFile(String filePath) {
        try {
            Path path = Path.of(filePath);
            Files.lines(path).forEach(this::addToQueue);
        } catch (IOException e) {
            showErrorMessage("An error occurred while reading the file.");
        }
    }

    private void highlightDownloadItem(String url, Color color) {
        SwingUtilities.invokeLater(() -> {
            downloadItemColors.put(url, color);
            downloadList.repaint();
        });
    }

    class DownloadTask implements Runnable {
        private String url;
        private DownloadManager downloadManager;

        public DownloadTask(String url, DownloadManager downloadManager) {
            this.url = url;
            this.downloadManager = downloadManager;
        }

        @Override
        public void run() {
            try {
                URL fileUrl = new URL(url); //url기반으로 객체 생성
                String fileName = getFileNameFromUrl(fileUrl);

                downloadImage(fileUrl, fileName);

                SwingUtilities.invokeLater(() -> {
                    downloadManager.notifyDownloadComplete(url, true);
                });
            } catch (IOException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    downloadManager.notifyDownloadComplete(url, false);
                });
            }
        }

        //url로부터 이미지를 다운로드 해서 파일에 저장
        private void downloadImage(URL imageUrl, String fileName) throws IOException {
            try (InputStream in = imageUrl.openStream()) {
                Path savePath = Path.of("download_result", fileName);
                Files.copy(in, savePath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Image file downloaded successfully");
            }
        }

        //url에서 파일 이름 추출해서 반환
        private String getFileNameFromUrl(URL url) {
            String urlString = url.toString();
            String fileName = urlString.substring(urlString.lastIndexOf('/') + 1);
            fileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "");
            return fileName;
        }
    }

    //클래스를 확장하여 리스트 셀의 렌더링을 사용자 정의하는 역할
    //DefaultListCellRenderer : Swing에서 JList와 같은 컴포넌트에서 리스트 셀의 기본 렌더링을 담당하는 클래스이다.
    class MyListCellRenderer extends DefaultListCellRenderer {
        @Override //getListCellRendererComponent
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String url = (String) value;
            Color color = downloadItemColors.getOrDefault(url, Color.WHITE);
            component.setBackground(color);
            return component;
        }
    }

    public static void main(String[] args) {
        //GUI 실행
        EventQueue.invokeLater(() -> {
            new DownloadManager();
        });
    }
}
