package com.timetracker.agent.ui;

import com.timetracker.agent.stats.ApplicationUsageSummary;
import com.timetracker.agent.stats.DailyUsageSummary;
import com.timetracker.agent.stats.StatisticsService;
import com.timetracker.agent.stats.StatisticsSnapshot;
import com.timetracker.agent.stats.StatsPeriod;
import com.timetracker.agent.util.DurationFormatter;
import com.timetracker.common.i18n.MessageCodes;
import com.timetracker.common.i18n.Messages;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.stream.Collectors;

/**
 * Вкладка локальной статистики: день / неделя / месяц / год / всё время.
 */
public final class StatisticsPanel extends JPanel {

    private final StatisticsService statisticsService;

    private final JComboBox<StatsPeriodItem> periodComboBox = new JComboBox<>();
    private final JLabel totalTimeValueLabel = new JLabel("0:00:00");
    private final DefaultListModel<String> dailyListModel = new DefaultListModel<>();
    private final DefaultListModel<String> applicationListModel = new DefaultListModel<>();

    public StatisticsPanel(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.BACKGROUND);
        buildContent();
        refresh();
    }

    private void buildContent() {
        periodComboBox.addItem(new StatsPeriodItem(StatsPeriod.DAY, MessageCodes.UI_STATS_PERIOD_DAY));
        periodComboBox.addItem(new StatsPeriodItem(StatsPeriod.WEEK, MessageCodes.UI_STATS_PERIOD_WEEK));
        periodComboBox.addItem(new StatsPeriodItem(StatsPeriod.MONTH, MessageCodes.UI_STATS_PERIOD_MONTH));
        periodComboBox.addItem(new StatsPeriodItem(StatsPeriod.YEAR, MessageCodes.UI_STATS_PERIOD_YEAR));
        periodComboBox.addItem(new StatsPeriodItem(StatsPeriod.ALL_TIME, MessageCodes.UI_STATS_PERIOD_ALL));
        periodComboBox.addActionListener(actionEvent -> refresh());

        JPanel headerCard = new JPanel(new BorderLayout(8, 10));
        UiTheme.styleSurfaceCard(headerCard);

        JPanel periodRow = new JPanel(new BorderLayout(8, 8));
        periodRow.setOpaque(false);
        JLabel periodLabel = new JLabel(Messages.get(MessageCodes.UI_STATS_PERIOD));
        UiTheme.styleMutedLabel(periodLabel);
        periodRow.add(periodLabel, BorderLayout.WEST);
        periodRow.add(periodComboBox, BorderLayout.CENTER);

        JPanel totalRow = new JPanel(new BorderLayout(8, 0));
        totalRow.setOpaque(false);
        JLabel totalCaptionLabel = new JLabel(Messages.get(MessageCodes.UI_STATS_TOTAL));
        UiTheme.styleMutedLabel(totalCaptionLabel);
        totalTimeValueLabel.setFont(totalTimeValueLabel.getFont().deriveFont(Font.BOLD, 22f));
        totalTimeValueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        totalRow.add(totalCaptionLabel, BorderLayout.WEST);
        totalRow.add(totalTimeValueLabel, BorderLayout.CENTER);

        headerCard.add(periodRow, BorderLayout.NORTH);
        headerCard.add(totalRow, BorderLayout.SOUTH);

        JPanel listsPanel = new JPanel(new GridLayout(1, 2, 12, 12));
        listsPanel.setOpaque(false);

        JPanel dailyPanel = createListCard(Messages.get(MessageCodes.UI_STATS_BY_DAY), dailyListModel);
        JPanel applicationsPanel = createListCard(Messages.get(MessageCodes.UI_STATS_BY_APP), applicationListModel);
        listsPanel.add(dailyPanel);
        listsPanel.add(applicationsPanel);

        add(headerCard, BorderLayout.NORTH);
        add(listsPanel, BorderLayout.CENTER);
    }

    private JPanel createListCard(String title, DefaultListModel<String> listModel) {
        JPanel panel = new JPanel(new BorderLayout(4, 8));
        UiTheme.styleSurfaceCard(panel);
        JLabel titleLabel = new JLabel(title);
        UiTheme.styleMutedLabel(titleLabel);
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JList<>(listModel)), BorderLayout.CENTER);
        return panel;
    }

    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        StatsPeriodItem selectedPeriodItem = (StatsPeriodItem) periodComboBox.getSelectedItem();
        StatsPeriod statsPeriod = selectedPeriodItem != null
                ? selectedPeriodItem.statsPeriod()
                : StatsPeriod.DAY;

        StatisticsSnapshot statisticsSnapshot = statisticsService.buildSnapshot(statsPeriod);
        totalTimeValueLabel.setText(
                DurationFormatter.formatSeconds(statisticsSnapshot.getTotalActiveSeconds())
        );

        dailyListModel.clear();
        statisticsSnapshot.getDailyUsageSummaries().stream()
                .map(this::formatDailyUsageLine)
                .collect(Collectors.toList())
                .forEach(dailyListModel::addElement);
        if (dailyListModel.isEmpty()) {
            dailyListModel.addElement(Messages.get(MessageCodes.UI_STATS_EMPTY));
        }

        applicationListModel.clear();
        statisticsSnapshot.getApplicationUsageSummaries().stream()
                .map(this::formatApplicationUsageLine)
                .collect(Collectors.toList())
                .forEach(applicationListModel::addElement);
        if (applicationListModel.isEmpty()) {
            applicationListModel.addElement(Messages.get(MessageCodes.UI_STATS_EMPTY));
        }
    }

    private String formatDailyUsageLine(DailyUsageSummary dailyUsageSummary) {
        return dailyUsageSummary.getDate()
                + " — "
                + DurationFormatter.formatSeconds(dailyUsageSummary.getDurationSeconds());
    }

    private String formatApplicationUsageLine(ApplicationUsageSummary applicationUsageSummary) {
        return applicationUsageSummary.getApplicationName()
                + " — "
                + DurationFormatter.formatSeconds(applicationUsageSummary.getDurationSeconds());
    }

    private record StatsPeriodItem(StatsPeriod statsPeriod, String messageCode) {
        @Override
        public String toString() {
            return Messages.get(messageCode);
        }
    }
}
