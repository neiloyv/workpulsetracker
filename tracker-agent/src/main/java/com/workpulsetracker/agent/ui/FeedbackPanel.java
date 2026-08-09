package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.feedback.FeedbackAttachment;
import com.workpulsetracker.agent.feedback.FeedbackCategory;
import com.workpulsetracker.agent.feedback.FeedbackSubmitService;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Вкладка Feedback: форма обращения, вложения с превью, диагностика.
 */
public final class FeedbackPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackPanel.class);
    private static final String schema = "local";
    private static final int MAX_ATTACHMENTS = 5;
    private static final long MAX_ATTACHMENT_BYTES = 5L * 1024L * 1024L;
    private static final int THUMBNAIL_SIZE = 72;
    private static final int BODY_HTML_WIDTH = 640;

    private final UserSettings userSettings;
    private final FeedbackSubmitService feedbackSubmitService;

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final JLabel emailLabel = new JLabel();
    private final JLabel categoryLabel = new JLabel();
    private final JLabel messageLabel = new JLabel();
    private final JLabel attachmentsLabel = new JLabel();
    private final JTextField emailField = new JTextField();
    private final JComboBox<CategoryItem> categoryComboBox = new JComboBox<>();
    private final JTextArea messageTextArea = new JTextArea(8, 40);
    private final JPanel attachmentsStripPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    private final JButton addAttachmentButton = new JButton();
    private final JCheckBox includeSystemInfoCheckBox = new JCheckBox();
    private final JButton sendButton = new JButton();
    private final JPanel formCard = new JPanel();
    private final List<FeedbackAttachment> feedbackAttachments = new ArrayList<>();

    public FeedbackPanel(
            UserSettings userSettings,
            FeedbackSubmitService feedbackSubmitService
    ) {
        this.userSettings = userSettings;
        this.feedbackSubmitService = feedbackSubmitService;
        setLayout(new BorderLayout());
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        buildContent();
        retranslate();
        reloadFromSettings();
    }

    private void buildContent() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(subtitleLabel);

        emailLabel.setForeground(UiTheme.TEXT_PRIMARY);
        categoryLabel.setForeground(UiTheme.TEXT_PRIMARY);
        messageLabel.setForeground(UiTheme.TEXT_PRIMARY);
        attachmentsLabel.setForeground(UiTheme.TEXT_PRIMARY);

        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        categoryComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        categoryComboBox.addItem(new CategoryItem(FeedbackCategory.BUG, MessageCodes.UI_FEEDBACK_CATEGORY_BUG));
        categoryComboBox.addItem(new CategoryItem(FeedbackCategory.FEATURE, MessageCodes.UI_FEEDBACK_CATEGORY_FEATURE));

        messageTextArea.setLineWrap(true);
        messageTextArea.setWrapStyleWord(true);
        messageTextArea.setBackground(UiTheme.SURFACE_2);
        messageTextArea.setForeground(UiTheme.TEXT_PRIMARY);
        messageTextArea.setCaretColor(UiTheme.TEXT_PRIMARY);
        messageTextArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane messageScrollPane = new JScrollPane(messageTextArea);
        messageScrollPane.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        messageScrollPane.setPreferredSize(new Dimension(100, 180));
        messageScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        attachmentsStripPanel.setOpaque(false);
        attachmentsStripPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane attachmentsScrollPane = new JScrollPane(attachmentsStripPanel);
        attachmentsScrollPane.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        attachmentsScrollPane.setPreferredSize(new Dimension(100, 120));
        attachmentsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        attachmentsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        attachmentsScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        attachmentsScrollPane.setTransferHandler(createAttachmentTransferHandler());
        attachmentsStripPanel.setTransferHandler(createAttachmentTransferHandler());

        UiTheme.styleSecondaryButton(addAttachmentButton);
        addAttachmentButton.addActionListener(actionEvent -> onAddAttachmentClicked());
        includeSystemInfoCheckBox.setOpaque(false);
        includeSystemInfoCheckBox.setForeground(UiTheme.TEXT_PRIMARY);
        includeSystemInfoCheckBox.setSelected(true);
        UiTheme.stylePrimaryButton(sendButton);
        sendButton.addActionListener(actionEvent -> onSendClicked());

        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        UiTheme.styleSurfaceCard(formCard);
        formCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(createCaption(emailLabel));
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(align(emailField));
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(createCaption(categoryLabel));
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(align(categoryComboBox));
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(createCaption(messageLabel));
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(messageScrollPane);
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(createCaption(attachmentsLabel));
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(attachmentsScrollPane);
        formCard.add(Box.createVerticalStrut(8));
        formCard.add(align(addAttachmentButton));
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(align(includeSystemInfoCheckBox));
        formCard.add(Box.createVerticalStrut(16));
        formCard.add(align(sendButton));

        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 16, 4));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(subtitleLabel);
        contentPanel.add(Box.createVerticalStrut(16));
        contentPanel.add(formCard);
        contentPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UiTheme.BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void retranslate() {
        titleLabel.setText(Messages.get(MessageCodes.UI_FEEDBACK_TITLE));
        subtitleLabel.setText(wrapHtml(Messages.get(MessageCodes.UI_FEEDBACK_SUBTITLE), BODY_HTML_WIDTH));
        emailLabel.setText(Messages.get(MessageCodes.UI_FEEDBACK_EMAIL));
        categoryLabel.setText(Messages.get(MessageCodes.UI_FEEDBACK_CATEGORY));
        messageLabel.setText(Messages.get(MessageCodes.UI_FEEDBACK_MESSAGE));
        attachmentsLabel.setText(Messages.get(MessageCodes.UI_FEEDBACK_ATTACHMENTS));
        addAttachmentButton.setText(Messages.get(MessageCodes.UI_FEEDBACK_ADD_ATTACHMENT));
        includeSystemInfoCheckBox.setText(Messages.get(MessageCodes.UI_FEEDBACK_INCLUDE_SYSTEM_INFO));
        sendButton.setText(Messages.get(MessageCodes.UI_FEEDBACK_SEND));
        categoryComboBox.repaint();
        rebuildAttachmentCards();
    }

    public void reloadFromSettings() {
        if (userSettings.getOperationMode().isNetworkSync() && StringUtils.isNotBlank(userSettings.getEmail())) {
            emailField.setText(userSettings.getEmail());
        } else if (StringUtils.isBlank(emailField.getText())) {
            emailField.setText("");
        }
    }

    private void onAddAttachmentClicked() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                Messages.get(MessageCodes.UI_FEEDBACK_ATTACHMENT_FILTER),
                "png", "jpg", "jpeg", "log", "txt"
        ));
        int chooserResult = fileChooser.showOpenDialog(this);
        if (chooserResult != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File[] selectedFiles = fileChooser.getSelectedFiles();
        if (Objects.isNull(selectedFiles)) {
            return;
        }
        for (File selectedFile : selectedFiles) {
            addAttachmentFile(selectedFile.toPath());
        }
    }

    private void addAttachmentFile(Path filePath) {
        if (feedbackAttachments.size() >= MAX_ATTACHMENTS) {
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_FEEDBACK_ATTACHMENTS_LIMIT, MAX_ATTACHMENTS),
                    Messages.get(MessageCodes.UI_FEEDBACK_TITLE),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        try {
            if (!Files.isRegularFile(filePath)) {
                return;
            }
            long fileSizeBytes = Files.size(filePath);
            if (fileSizeBytes <= 0L || fileSizeBytes > MAX_ATTACHMENT_BYTES) {
                UiDialogs.showMessage(
                        Messages.get(MessageCodes.UI_FEEDBACK_ATTACHMENT_TOO_LARGE),
                        Messages.get(MessageCodes.UI_FEEDBACK_TITLE),
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            String fileName = filePath.getFileName().toString();
            String lowerFileName = fileName.toLowerCase(Locale.ROOT);
            boolean image = lowerFileName.endsWith(".png")
                    || lowerFileName.endsWith(".jpg")
                    || lowerFileName.endsWith(".jpeg");
            boolean allowedLog = lowerFileName.endsWith(".log") || lowerFileName.endsWith(".txt");
            if (!image && !allowedLog) {
                UiDialogs.showMessage(
                        Messages.get(MessageCodes.UI_FEEDBACK_ATTACHMENT_TYPE_INVALID),
                        Messages.get(MessageCodes.UI_FEEDBACK_TITLE),
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            boolean alreadyAdded = feedbackAttachments.stream()
                    .anyMatch(feedbackAttachment -> feedbackAttachment.getFilePath().equals(filePath));
            if (alreadyAdded) {
                return;
            }
            String contentType = image
                    ? (lowerFileName.endsWith(".png") ? "image/png" : "image/jpeg")
                    : "text/plain";
            feedbackAttachments.add(new FeedbackAttachment(filePath, fileName, fileSizeBytes, contentType, image));
            rebuildAttachmentCards();
        } catch (Exception exception) {
            logger.warn("schema={} Failed to add attachment: {}", schema, exception.getMessage());
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_FEEDBACK_ATTACHMENT_ADD_FAILED, exception.getMessage()),
                    Messages.get(MessageCodes.UI_FEEDBACK_TITLE),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void rebuildAttachmentCards() {
        attachmentsStripPanel.removeAll();
        feedbackAttachments.forEach(feedbackAttachment ->
                attachmentsStripPanel.add(createAttachmentCard(feedbackAttachment))
        );
        if (feedbackAttachments.isEmpty()) {
            JLabel emptyLabel = new JLabel(Messages.get(MessageCodes.UI_FEEDBACK_ATTACHMENTS_HINT));
            UiTheme.styleMutedLabel(emptyLabel);
            attachmentsStripPanel.add(emptyLabel);
        }
        attachmentsStripPanel.revalidate();
        attachmentsStripPanel.repaint();
    }

    private JPanel createAttachmentCard(FeedbackAttachment feedbackAttachment) {
        JPanel cardPanel = new JPanel(new BorderLayout(6, 4));
        cardPanel.setBackground(UiTheme.SURFACE_2);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        cardPanel.setPreferredSize(new Dimension(150, 110));
        cardPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel thumbnailLabel = new JLabel();
        thumbnailLabel.setHorizontalAlignment(SwingConstants.CENTER);
        thumbnailLabel.setPreferredSize(new Dimension(THUMBNAIL_SIZE, THUMBNAIL_SIZE));
        if (feedbackAttachment.isImage()) {
            thumbnailLabel.setIcon(loadThumbnailIcon(feedbackAttachment.getFilePath()));
        } else {
            thumbnailLabel.setText("LOG");
            thumbnailLabel.setForeground(UiTheme.TEXT_PRIMARY);
            thumbnailLabel.setFont(thumbnailLabel.getFont().deriveFont(Font.BOLD, 14f));
        }

        JLabel nameLabel = new JLabel(truncate(feedbackAttachment.getFileName(), 18));
        nameLabel.setForeground(UiTheme.TEXT_PRIMARY);
        JLabel sizeLabel = new JLabel(formatFileSize(feedbackAttachment.getFileSizeBytes()));
        UiTheme.styleMutedLabel(sizeLabel);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(nameLabel);
        textPanel.add(sizeLabel);

        JButton removeButton = new JButton("×");
        removeButton.setFocusPainted(false);
        removeButton.setMargin(new java.awt.Insets(0, 6, 0, 6));
        removeButton.addActionListener(actionEvent -> {
            feedbackAttachments.remove(feedbackAttachment);
            rebuildAttachmentCards();
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(thumbnailLabel, BorderLayout.CENTER);
        topPanel.add(removeButton, BorderLayout.EAST);

        cardPanel.add(topPanel, BorderLayout.CENTER);
        cardPanel.add(textPanel, BorderLayout.SOUTH);
        cardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() >= 1 && mouseEvent.getButton() == MouseEvent.BUTTON1) {
                    Component deepComponent = SwingUtilities.getDeepestComponentAt(
                            cardPanel,
                            mouseEvent.getX(),
                            mouseEvent.getY()
                    );
                    if (deepComponent == removeButton || SwingUtilities.isDescendingFrom(deepComponent, removeButton)) {
                        return;
                    }
                    showAttachmentPreview(feedbackAttachment);
                }
            }
        });
        return cardPanel;
    }

    private void showAttachmentPreview(FeedbackAttachment feedbackAttachment) {
        Frame ownerFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog previewDialog = new JDialog(ownerFrame, feedbackAttachment.getFileName(), true);
        previewDialog.setLayout(new BorderLayout(8, 8));
        previewDialog.getContentPane().setBackground(UiTheme.BACKGROUND);
        try {
            if (feedbackAttachment.isImage()) {
                BufferedImage bufferedImage = ImageIO.read(feedbackAttachment.getFilePath().toFile());
                if (Objects.isNull(bufferedImage)) {
                    throw new IllegalStateException("Unable to decode image");
                }
                Image scaledImage = scaleToFit(bufferedImage, 900, 700);
                JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
                imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                previewDialog.add(new JScrollPane(imageLabel), BorderLayout.CENTER);
            } else {
                String fileText = Files.readString(feedbackAttachment.getFilePath(), StandardCharsets.UTF_8);
                if (fileText.length() > 50_000) {
                    fileText = fileText.substring(0, 50_000) + "\n...";
                }
                JTextArea previewTextArea = new JTextArea(fileText);
                previewTextArea.setEditable(false);
                previewTextArea.setBackground(UiTheme.SURFACE);
                previewTextArea.setForeground(UiTheme.TEXT_PRIMARY);
                previewDialog.add(new JScrollPane(previewTextArea), BorderLayout.CENTER);
            }
        } catch (Exception exception) {
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_FEEDBACK_PREVIEW_FAILED, exception.getMessage()),
                    Messages.get(MessageCodes.UI_FEEDBACK_TITLE),
                    JOptionPane.ERROR_MESSAGE
            );
            previewDialog.dispose();
            return;
        }
        JButton closeButton = new JButton(Messages.get(MessageCodes.UI_FEEDBACK_PREVIEW_CLOSE));
        UiTheme.styleSecondaryButton(closeButton);
        closeButton.addActionListener(actionEvent -> previewDialog.dispose());
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.setOpaque(false);
        southPanel.add(closeButton);
        previewDialog.add(southPanel, BorderLayout.SOUTH);
        previewDialog.setSize(960, 720);
        previewDialog.setLocationRelativeTo(null);
        previewDialog.setVisible(true);
        previewDialog.dispose();
    }

    private void onSendClicked() {
        String replyEmail = StringUtils.trimToEmpty(emailField.getText());
        String message = StringUtils.trimToEmpty(messageTextArea.getText());
        if (StringUtils.isBlank(replyEmail) || !replyEmail.contains("@")) {
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_FEEDBACK_VALIDATION_EMAIL),
                    Messages.get(MessageCodes.UI_FEEDBACK_TITLE),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        if (StringUtils.isBlank(message)) {
            UiDialogs.showMessage(
                    Messages.get(MessageCodes.UI_FEEDBACK_VALIDATION_MESSAGE),
                    Messages.get(MessageCodes.UI_FEEDBACK_TITLE),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        CategoryItem categoryItem = (CategoryItem) categoryComboBox.getSelectedItem();
        FeedbackCategory feedbackCategory = Objects.nonNull(categoryItem)
                ? categoryItem.feedbackCategory()
                : FeedbackCategory.BUG;

        sendButton.setEnabled(false);
        SwingWorker<FeedbackSubmitService.FeedbackSubmitResult, Void> swingWorker = new SwingWorker<>() {
            @Override
            protected FeedbackSubmitService.FeedbackSubmitResult doInBackground() throws Exception {
                return feedbackSubmitService.submit(
                        userSettings,
                        replyEmail,
                        feedbackCategory,
                        message,
                        includeSystemInfoCheckBox.isSelected(),
                        List.copyOf(feedbackAttachments)
                );
            }

            @Override
            protected void done() {
                sendButton.setEnabled(true);
                try {
                    FeedbackSubmitService.FeedbackSubmitResult feedbackSubmitResult = get();
                    if (feedbackSubmitResult.apiSubmitted()) {
                        UiDialogs.showMessage(
                                Messages.get(MessageCodes.UI_FEEDBACK_SEND_SUCCESS_API),
                                Messages.get(MessageCodes.UI_FEEDBACK_TITLE),
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        messageTextArea.setText("");
                        feedbackAttachments.clear();
                        rebuildAttachmentCards();
                    } else {
                        String attachmentsHint = feedbackSubmitResult.describeAttachmentsHint();
                        String successMessage = StringUtils.isNotBlank(attachmentsHint)
                                ? Messages.get(MessageCodes.UI_FEEDBACK_SEND_SUCCESS_MAILTO_ATTACHMENTS, attachmentsHint)
                                : Messages.get(MessageCodes.UI_FEEDBACK_SEND_SUCCESS_MAILTO);
                        UiDialogs.showMessage(
                                successMessage,
                                Messages.get(MessageCodes.UI_FEEDBACK_TITLE),
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                } catch (Exception exception) {
                    Throwable cause = Objects.nonNull(exception.getCause()) ? exception.getCause() : exception;
                    logger.error("schema={} Feedback send failed: {}", schema, cause.getMessage(), cause);
                    UiDialogs.showMessage(
                            Messages.get(MessageCodes.UI_FEEDBACK_SEND_FAILED, cause.getMessage()),
                            Messages.get(MessageCodes.UI_FEEDBACK_TITLE),
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        swingWorker.execute();
    }

    private TransferHandler createAttachmentTransferHandler() {
        return new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport transferSupport) {
                return transferSupport.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport transferSupport) {
                if (!canImport(transferSupport)) {
                    return false;
                }
                try {
                    Transferable transferable = transferSupport.getTransferable();
                    List<File> transferredFiles =
                            (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    transferredFiles.forEach(file -> addAttachmentFile(file.toPath()));
                    return true;
                } catch (Exception exception) {
                    logger.warn("schema={} Drag-and-drop import failed: {}", schema, exception.getMessage());
                    return false;
                }
            }
        };
    }

    private static ImageIcon loadThumbnailIcon(Path imagePath) {
        try {
            BufferedImage bufferedImage = ImageIO.read(imagePath.toFile());
            if (Objects.isNull(bufferedImage)) {
                return null;
            }
            Image scaledImage = scaleToFit(bufferedImage, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
            return new ImageIcon(scaledImage);
        } catch (Exception exception) {
            return null;
        }
    }

    private static Image scaleToFit(BufferedImage bufferedImage, int maxWidth, int maxHeight) {
        double widthScale = (double) maxWidth / bufferedImage.getWidth();
        double heightScale = (double) maxHeight / bufferedImage.getHeight();
        double scale = Math.min(1.0d, Math.min(widthScale, heightScale));
        int targetWidth = Math.max(1, (int) Math.round(bufferedImage.getWidth() * scale));
        int targetHeight = Math.max(1, (int) Math.round(bufferedImage.getHeight() * scale));
        return bufferedImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
    }

    private static String formatFileSize(long fileSizeBytes) {
        if (fileSizeBytes < 1024L) {
            return fileSizeBytes + " B";
        }
        if (fileSizeBytes < 1024L * 1024L) {
            return (fileSizeBytes / 1024L) + " KB";
        }
        return String.format(Locale.ROOT, "%.1f MB", fileSizeBytes / (1024.0d * 1024.0d));
    }

    private static String truncate(String value, int maxLength) {
        if (StringUtils.isBlank(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private static JLabel createCaption(JLabel captionLabel) {
        captionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return captionLabel;
    }

    private static JComponent align(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    private static String wrapHtml(String text, int widthPixels) {
        return "<html><body style='width:" + widthPixels + "px'>" + text + "</body></html>";
    }

    private record CategoryItem(FeedbackCategory feedbackCategory, String messageCode) {
        @Override
        public String toString() {
            return Messages.get(messageCode);
        }
    }
}
