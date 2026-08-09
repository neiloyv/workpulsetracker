package com.workpulsetracker.agent.ui;

import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Вкладка Info / Help: краткий onboarding по продукту, таймлайну, категориям и Pomodoro.
 */
public final class InfoPanel extends JPanel {

    private static final int BODY_HTML_WIDTH = 720;
    private static final int CARD_HTML_WIDTH = 280;

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final JLabel privacyTitleLabel = new JLabel();
    private final JLabel privacyBodyLabel = new JLabel();
    private final JLabel timelineTitleLabel = new JLabel();
    private final JLabel timelineBodyLabel = new JLabel();
    private final JLabel activeStateTitleLabel = new JLabel();
    private final JLabel activeStateBodyLabel = new JLabel();
    private final JLabel idleStateTitleLabel = new JLabel();
    private final JLabel idleStateBodyLabel = new JLabel();
    private final JLabel excludedStateTitleLabel = new JLabel();
    private final JLabel excludedStateBodyLabel = new JLabel();
    private final JLabel categoriesTitleLabel = new JLabel();
    private final JLabel categoriesBodyLabel = new JLabel();
    private final JLabel pomodoroTitleLabel = new JLabel();
    private final JLabel pomodoroBodyLabel = new JLabel();

    private final JPanel privacyCard = new JPanel(new BorderLayout(0, 8));
    private final JPanel timelineCard = new JPanel();
    private final JPanel activeStateCard = new JPanel(new BorderLayout(0, 8));
    private final JPanel idleStateCard = new JPanel(new BorderLayout(0, 8));
    private final JPanel excludedStateCard = new JPanel(new BorderLayout(0, 8));
    private final JPanel categoriesCard = new JPanel(new BorderLayout(0, 8));
    private final JPanel pomodoroCard = new JPanel(new BorderLayout(0, 8));
    private final JPanel contentPanel = new JPanel();
    private final List<JPanel> surfaceCards = new ArrayList<>();

    public InfoPanel() {
        setLayout(new BorderLayout());
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        buildContent();
        retranslate();
    }

    private void buildContent() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        UiTheme.styleMutedLabel(subtitleLabel);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        styleSectionTitle(privacyTitleLabel);
        styleSectionTitle(timelineTitleLabel);
        styleSectionTitle(categoriesTitleLabel);
        styleSectionTitle(pomodoroTitleLabel);
        styleCardTitle(activeStateTitleLabel);
        styleCardTitle(idleStateTitleLabel);
        styleCardTitle(excludedStateTitleLabel);

        styleBodyLabel(privacyBodyLabel);
        styleBodyLabel(timelineBodyLabel);
        styleBodyLabel(categoriesBodyLabel);
        styleBodyLabel(pomodoroBodyLabel);
        styleBodyLabel(activeStateBodyLabel);
        styleBodyLabel(idleStateBodyLabel);
        styleBodyLabel(excludedStateBodyLabel);

        configureSurfaceCard(privacyCard);
        configureSurfaceCard(timelineCard);
        configureSurfaceCard(activeStateCard);
        configureSurfaceCard(idleStateCard);
        configureSurfaceCard(excludedStateCard);
        configureSurfaceCard(categoriesCard);
        configureSurfaceCard(pomodoroCard);

        privacyCard.add(privacyTitleLabel, BorderLayout.NORTH);
        privacyCard.add(privacyBodyLabel, BorderLayout.CENTER);

        timelineCard.setLayout(new BoxLayout(timelineCard, BoxLayout.Y_AXIS));
        timelineTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        timelineBodyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        timelineCard.add(timelineTitleLabel);
        timelineCard.add(Box.createVerticalStrut(6));
        timelineCard.add(timelineBodyLabel);
        timelineCard.add(Box.createVerticalStrut(14));

        JPanel timelineStatesPanel = new JPanel(new GridLayout(1, 3, 12, 0));
        timelineStatesPanel.setOpaque(false);
        timelineStatesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        timelineStatesPanel.add(buildStateCard(
                activeStateCard,
                DayActivityTimelinePanel.activeColor(),
                activeStateTitleLabel,
                activeStateBodyLabel
        ));
        timelineStatesPanel.add(buildStateCard(
                idleStateCard,
                DayActivityTimelinePanel.idleColor(),
                idleStateTitleLabel,
                idleStateBodyLabel
        ));
        timelineStatesPanel.add(buildStateCard(
                excludedStateCard,
                DayActivityTimelinePanel.excludedColor(),
                excludedStateTitleLabel,
                excludedStateBodyLabel
        ));
        timelineCard.add(timelineStatesPanel);

        categoriesCard.add(categoriesTitleLabel, BorderLayout.NORTH);
        categoriesCard.add(categoriesBodyLabel, BorderLayout.CENTER);

        pomodoroCard.add(pomodoroTitleLabel, BorderLayout.NORTH);
        pomodoroCard.add(pomodoroBodyLabel, BorderLayout.CENTER);

        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 16, 4));
        addAligned(contentPanel, titleLabel);
        contentPanel.add(Box.createVerticalStrut(8));
        addAligned(contentPanel, subtitleLabel);
        contentPanel.add(Box.createVerticalStrut(18));
        addAligned(contentPanel, privacyCard);
        contentPanel.add(Box.createVerticalStrut(12));
        addAligned(contentPanel, timelineCard);
        contentPanel.add(Box.createVerticalStrut(12));
        addAligned(contentPanel, categoriesCard);
        contentPanel.add(Box.createVerticalStrut(12));
        addAligned(contentPanel, pomodoroCard);
        contentPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UiTheme.BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel buildStateCard(
            JPanel stateCard,
            Color swatchColor,
            JLabel titleLabel,
            JLabel bodyLabel
    ) {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerPanel.setOpaque(false);
        headerPanel.add(createColorSwatch(swatchColor));
        headerPanel.add(titleLabel);

        stateCard.removeAll();
        stateCard.setLayout(new BorderLayout(0, 8));
        stateCard.add(headerPanel, BorderLayout.NORTH);
        stateCard.add(bodyLabel, BorderLayout.CENTER);
        return stateCard;
    }

    private static JPanel createColorSwatch(Color color) {
        JPanel colorSwatch = new JPanel();
        colorSwatch.setOpaque(true);
        colorSwatch.setBackground(color);
        Dimension swatchSize = new Dimension(14, 14);
        colorSwatch.setPreferredSize(swatchSize);
        colorSwatch.setMinimumSize(swatchSize);
        colorSwatch.setMaximumSize(swatchSize);
        return colorSwatch;
    }

    private void configureSurfaceCard(JPanel cardPanel) {
        UiTheme.styleSurfaceCard(cardPanel);
        cardPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        surfaceCards.add(cardPanel);
    }

    private static void addAligned(JPanel parentPanel, Component component) {
        if (component instanceof JComponent jComponent) {
            jComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        parentPanel.add(component);
    }

    private static void styleSectionTitle(JLabel label) {
        label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
        label.setForeground(UiTheme.TEXT_PRIMARY);
    }

    private static void styleCardTitle(JLabel label) {
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        label.setForeground(UiTheme.TEXT_PRIMARY);
        label.setVerticalAlignment(SwingConstants.CENTER);
    }

    private static void styleBodyLabel(JLabel label) {
        UiTheme.styleMutedLabel(label);
        label.setVerticalAlignment(SwingConstants.TOP);
    }

    public void retranslate() {
        titleLabel.setText(Messages.get(MessageCodes.UI_INFO_TITLE));
        subtitleLabel.setText(wrapHtml(Messages.get(MessageCodes.UI_INFO_SUBTITLE), BODY_HTML_WIDTH));
        privacyTitleLabel.setText(Messages.get(MessageCodes.UI_INFO_PRIVACY_TITLE));
        privacyBodyLabel.setText(wrapHtml(Messages.get(MessageCodes.UI_INFO_PRIVACY_BODY), BODY_HTML_WIDTH));
        timelineTitleLabel.setText(Messages.get(MessageCodes.UI_INFO_TIMELINE_TITLE));
        timelineBodyLabel.setText(wrapHtml(Messages.get(MessageCodes.UI_INFO_TIMELINE_BODY), BODY_HTML_WIDTH));
        activeStateTitleLabel.setText(Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_ACTIVE));
        activeStateBodyLabel.setText(wrapHtml(Messages.get(MessageCodes.UI_INFO_TIMELINE_ACTIVE_BODY), CARD_HTML_WIDTH));
        idleStateTitleLabel.setText(Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_IDLE));
        idleStateBodyLabel.setText(wrapHtml(Messages.get(MessageCodes.UI_INFO_TIMELINE_IDLE_BODY), CARD_HTML_WIDTH));
        excludedStateTitleLabel.setText(Messages.get(MessageCodes.UI_MAIN_TIMELINE_STATE_EXCLUDED));
        excludedStateBodyLabel.setText(wrapHtml(Messages.get(MessageCodes.UI_INFO_TIMELINE_EXCLUDED_BODY), CARD_HTML_WIDTH));
        categoriesTitleLabel.setText(Messages.get(MessageCodes.UI_INFO_CATEGORIES_TITLE));
        categoriesBodyLabel.setText(wrapHtml(Messages.get(MessageCodes.UI_INFO_CATEGORIES_BODY), BODY_HTML_WIDTH));
        pomodoroTitleLabel.setText(Messages.get(MessageCodes.UI_INFO_POMODORO_TITLE));
        pomodoroBodyLabel.setText(wrapHtml(Messages.get(MessageCodes.UI_INFO_POMODORO_BODY), BODY_HTML_WIDTH));
        revalidate();
        repaint();
    }

    public void applyTheme() {
        setBackground(UiTheme.BACKGROUND);
        titleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        UiTheme.styleMutedLabel(subtitleLabel);
        styleSectionTitle(privacyTitleLabel);
        styleSectionTitle(timelineTitleLabel);
        styleSectionTitle(categoriesTitleLabel);
        styleSectionTitle(pomodoroTitleLabel);
        styleCardTitle(activeStateTitleLabel);
        styleCardTitle(idleStateTitleLabel);
        styleCardTitle(excludedStateTitleLabel);
        styleBodyLabel(privacyBodyLabel);
        styleBodyLabel(timelineBodyLabel);
        styleBodyLabel(categoriesBodyLabel);
        styleBodyLabel(pomodoroBodyLabel);
        styleBodyLabel(activeStateBodyLabel);
        styleBodyLabel(idleStateBodyLabel);
        styleBodyLabel(excludedStateBodyLabel);
        surfaceCards.forEach(UiTheme::styleSurfaceCard);
    }

    private static String wrapHtml(String text, int widthPixels) {
        return "<html><body style='width:" + widthPixels + "px'>" + text + "</body></html>";
    }
}
