package com.workpulsetracker.agent.ui;

import com.workpulsetracker.agent.stats.StatisticsService;
import com.workpulsetracker.agent.storage.ProgramCategoryIds;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.storage.UserSettingsStore;
import com.workpulsetracker.agent.util.ProgramCategoryDisplayNames;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Вкладка Programs: категории приложений и Track toggle (exclude из обычного учёта).
 */
public final class ProgramsPanel extends JPanel {

    private final StatisticsService statisticsService;
    private final UserSettings userSettings;
    private final UserSettingsStore userSettingsStore;
    private final Runnable programsChangedListener;

    private final JLabel titleLabel = new JLabel();
    private final JLabel categoriesTitleLabel = new JLabel();
    private final JLabel emptyLabel = new JLabel();
    private final ProgramsTableModel programsTableModel = new ProgramsTableModel();
    private final JTable programsTable = new JTable(programsTableModel);
    private final JScrollPane programsScrollPane = new JScrollPane(programsTable);
    private final JPanel emptyStatePanel = new JPanel(new GridBagLayout());
    private final JPanel tableCard = new JPanel(new BorderLayout(0, 8));
    private final DefaultListModel<String> categoriesListModel = new DefaultListModel<>();
    private final JList<String> categoriesList = new JList<>(categoriesListModel);
    private final JButton addCategoryButton = new JButton();
    private final JButton removeCategoryButton = new JButton();
    private final JPanel categoriesCard = new JPanel(new BorderLayout(0, 8));
    private final JComboBox<CategoryItem> categoryEditorComboBox = new JComboBox<>();
    private final CategoryCellEditor categoryCellEditor = new CategoryCellEditor(categoryEditorComboBox);
    private final TrackToggleCellEditor trackToggleCellEditor = new TrackToggleCellEditor();
    private boolean suppressChangeEvents;

    public ProgramsPanel(
            StatisticsService statisticsService,
            UserSettings userSettings,
            UserSettingsStore userSettingsStore,
            Runnable programsChangedListener
    ) {
        this.statisticsService = statisticsService;
        this.userSettings = userSettings;
        this.userSettingsStore = userSettingsStore;
        this.programsChangedListener = Objects.requireNonNullElse(programsChangedListener, () -> {
        });
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiTheme.BACKGROUND);
        buildContent();
        retranslate();
        refresh();
    }

    private void buildContent() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(UiTheme.TEXT_PRIMARY);

        UiTheme.styleSurfaceCard(tableCard);
        UiTheme.styleUsageTable(programsTable);
        programsTable.setRowHeight(40);
        programsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        programsTable.getTableHeader().setReorderingAllowed(false);
        programsTable.setFillsViewportHeight(true);
        configureProgramsTableColumns();

        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setVerticalAlignment(SwingConstants.CENTER);
        emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.PLAIN, 15f));
        UiTheme.styleMutedLabel(emptyLabel);
        emptyStatePanel.setOpaque(false);
        emptyStatePanel.add(emptyLabel);

        tableCard.add(programsScrollPane, BorderLayout.CENTER);

        UiTheme.styleSurfaceCard(categoriesCard);
        categoriesTitleLabel.setFont(categoriesTitleLabel.getFont().deriveFont(Font.BOLD, 16f));
        categoriesTitleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        categoriesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoriesList.setBackground(UiTheme.SURFACE);
        categoriesList.setForeground(UiTheme.TEXT_PRIMARY);
        categoriesList.setCellRenderer(new CategoryListCellRenderer());
        categoriesList.addListSelectionListener(listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {
                updateRemoveCategoryButtonState();
            }
        });

        UiTheme.styleSecondaryButton(addCategoryButton);
        UiTheme.styleSecondaryButton(removeCategoryButton);
        addCategoryButton.addActionListener(actionEvent -> onAddCategoryClicked());
        removeCategoryButton.addActionListener(actionEvent -> onRemoveCategoryClicked());

        JPanel categoryButtonsPanel = new JPanel();
        categoryButtonsPanel.setOpaque(false);
        categoryButtonsPanel.setLayout(new BoxLayout(categoryButtonsPanel, BoxLayout.Y_AXIS));
        addCategoryButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        removeCategoryButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        addCategoryButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        removeCategoryButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        categoryButtonsPanel.add(addCategoryButton);
        categoryButtonsPanel.add(Box.createVerticalStrut(8));
        categoryButtonsPanel.add(removeCategoryButton);

        JPanel categoriesHeaderPanel = new JPanel(new BorderLayout(0, 8));
        categoriesHeaderPanel.setOpaque(false);
        categoriesHeaderPanel.add(categoriesTitleLabel, BorderLayout.NORTH);
        categoriesHeaderPanel.add(new JScrollPane(categoriesList), BorderLayout.CENTER);
        categoriesHeaderPanel.add(categoryButtonsPanel, BorderLayout.SOUTH);
        categoriesCard.add(categoriesHeaderPanel, BorderLayout.CENTER);

        // Предпочтительные размеры обнуляем, чтобы weightx задал ровно 2/3 и 1/3.
        tableCard.setPreferredSize(new Dimension(0, 0));
        categoriesCard.setPreferredSize(new Dimension(0, 0));

        JPanel bodyPanel = new JPanel(new GridBagLayout());
        bodyPanel.setOpaque(false);

        GridBagConstraints tableConstraints = new GridBagConstraints();
        tableConstraints.gridx = 0;
        tableConstraints.gridy = 0;
        tableConstraints.weightx = 2;
        tableConstraints.weighty = 1;
        tableConstraints.fill = GridBagConstraints.BOTH;
        tableConstraints.insets = new Insets(0, 0, 0, 12);
        bodyPanel.add(tableCard, tableConstraints);

        GridBagConstraints categoriesConstraints = new GridBagConstraints();
        categoriesConstraints.gridx = 1;
        categoriesConstraints.gridy = 0;
        categoriesConstraints.weightx = 1;
        categoriesConstraints.weighty = 1;
        categoriesConstraints.fill = GridBagConstraints.BOTH;
        categoriesConstraints.insets = new Insets(0, 0, 0, 0);
        bodyPanel.add(categoriesCard, categoriesConstraints);

        add(titleLabel, BorderLayout.NORTH);
        add(bodyPanel, BorderLayout.CENTER);
    }

    private void configureProgramsTableColumns() {
        programsTable.getColumnModel().getColumn(0).setPreferredWidth(280);
        programsTable.getColumnModel().getColumn(0).setCellRenderer(new ApplicationNameCellRenderer());

        refreshCategoryEditorItems();
        categoryEditorComboBox.setRenderer(new CategoryComboBoxRenderer());
        programsTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        programsTable.getColumnModel().getColumn(1).setCellEditor(categoryCellEditor);
        programsTable.getColumnModel().getColumn(1).setCellRenderer(new CategoryCellRenderer());

        programsTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        programsTable.getColumnModel().getColumn(2).setMaxWidth(110);
        programsTable.getColumnModel().getColumn(2).setCellRenderer(new TrackToggleCellRenderer());
        programsTable.getColumnModel().getColumn(2).setCellEditor(trackToggleCellEditor);
    }

    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }
        suppressChangeEvents = true;
        try {
            List<String> programApplicationKeys = statisticsService.listKnownProgramApplicationKeys();
            programsTableModel.setProgramApplicationKeys(programApplicationKeys);
            refreshCategoriesList();
            refreshCategoryEditorItems();
            boolean empty = programApplicationKeys.isEmpty();
            tableCard.removeAll();
            tableCard.add(empty ? emptyStatePanel : programsScrollPane, BorderLayout.CENTER);
            tableCard.revalidate();
            tableCard.repaint();
            updateRemoveCategoryButtonState();
        } finally {
            suppressChangeEvents = false;
        }
    }

    public void retranslate() {
        titleLabel.setText(Messages.get(MessageCodes.UI_TAB_PROGRAMS));
        categoriesTitleLabel.setText(Messages.get(MessageCodes.UI_PROGRAMS_CATEGORIES));
        emptyLabel.setText(Messages.get(MessageCodes.UI_PROGRAMS_EMPTY));
        addCategoryButton.setText(Messages.get(MessageCodes.UI_PROGRAMS_ADD_CATEGORY));
        removeCategoryButton.setText(Messages.get(MessageCodes.UI_PROGRAMS_REMOVE_CATEGORY));
        programsTableModel.retranslate();
        refreshCategoryEditorItems();
        refreshCategoriesList();
        configureProgramsTableColumns();
        programsTable.repaint();
        categoriesList.repaint();
    }

    private void refreshCategoriesList() {
        String selectedCategoryId = categoriesList.getSelectedValue();
        categoriesListModel.clear();
        userSettings.listAllProgramCategoryIds().forEach(categoriesListModel::addElement);
        if (StringUtils.isNotBlank(selectedCategoryId)) {
            for (int index = 0; index < categoriesListModel.size(); index++) {
                if (categoriesListModel.get(index).equalsIgnoreCase(selectedCategoryId)) {
                    categoriesList.setSelectedIndex(index);
                    break;
                }
            }
        }
    }

    private void refreshCategoryEditorItems() {
        Object selectedItem = categoryEditorComboBox.getSelectedItem();
        categoryEditorComboBox.removeAllItems();
        userSettings.listAllProgramCategoryIds().stream()
                .map(CategoryItem::new)
                .forEach(categoryEditorComboBox::addItem);
        if (selectedItem instanceof CategoryItem selectedCategoryItem) {
            for (int index = 0; index < categoryEditorComboBox.getItemCount(); index++) {
                CategoryItem categoryItem = categoryEditorComboBox.getItemAt(index);
                if (categoryItem.categoryId().equalsIgnoreCase(selectedCategoryItem.categoryId())) {
                    categoryEditorComboBox.setSelectedIndex(index);
                    break;
                }
            }
        }
    }

    private void updateRemoveCategoryButtonState() {
        String selectedCategoryId = categoriesList.getSelectedValue();
        removeCategoryButton.setEnabled(
                StringUtils.isNotBlank(selectedCategoryId)
                        && !ProgramCategoryIds.isDefaultCategoryId(selectedCategoryId)
        );
    }

    private void onAddCategoryClicked() {
        String categoryName = JOptionPane.showInputDialog(
                this,
                Messages.get(MessageCodes.UI_PROGRAMS_ADD_CATEGORY_PROMPT),
                Messages.get(MessageCodes.UI_PROGRAMS_ADD_CATEGORY),
                JOptionPane.PLAIN_MESSAGE
        );
        if (StringUtils.isBlank(categoryName)) {
            return;
        }
        boolean added = userSettings.addCustomProgramCategory(categoryName);
        if (!added) {
            return;
        }
        persistAndNotify();
        refreshCategoriesList();
        refreshCategoryEditorItems();
        programsTable.repaint();
    }

    private void onRemoveCategoryClicked() {
        String selectedCategoryId = categoriesList.getSelectedValue();
        if (StringUtils.isBlank(selectedCategoryId) || ProgramCategoryIds.isDefaultCategoryId(selectedCategoryId)) {
            return;
        }
        int confirmationResult = JOptionPane.showConfirmDialog(
                this,
                Messages.get(
                        MessageCodes.UI_PROGRAMS_REMOVE_CATEGORY_CONFIRM,
                        ProgramCategoryDisplayNames.resolveDisplayName(selectedCategoryId)
                ),
                Messages.get(MessageCodes.UI_PROGRAMS_REMOVE_CATEGORY),
                JOptionPane.YES_NO_OPTION
        );
        if (confirmationResult != JOptionPane.YES_OPTION) {
            return;
        }
        if (!userSettings.removeCustomProgramCategory(selectedCategoryId)) {
            return;
        }
        persistAndNotify();
        refresh();
    }

    private void persistAndNotify() {
        userSettingsStore.save(userSettings);
        programsChangedListener.run();
    }

    private final class ProgramsTableModel extends AbstractTableModel {

        private final List<String> programApplicationKeys = new ArrayList<>();
        private String applicationColumnName = "";
        private String categoryColumnName = "";
        private String trackColumnName = "";

        void setProgramApplicationKeys(List<String> programApplicationKeys) {
            this.programApplicationKeys.clear();
            if (Objects.nonNull(programApplicationKeys)) {
                this.programApplicationKeys.addAll(programApplicationKeys);
            }
            fireTableDataChanged();
        }

        void retranslate() {
            applicationColumnName = Messages.get(MessageCodes.UI_TABLE_APPLICATION);
            categoryColumnName = Messages.get(MessageCodes.UI_PROGRAMS_COLUMN_CATEGORY);
            trackColumnName = Messages.get(MessageCodes.UI_PROGRAMS_COLUMN_TRACK);
            fireTableStructureChanged();
        }

        @Override
        public int getRowCount() {
            return programApplicationKeys.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> applicationColumnName;
                case 1 -> categoryColumnName;
                case 2 -> trackColumnName;
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 2 -> Boolean.class;
                default -> String.class;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1 || columnIndex == 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String programApplicationKey = programApplicationKeys.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> programApplicationKey;
                case 1 -> userSettings.getApplicationCategoryId(programApplicationKey);
                case 2 -> userSettings.isApplicationTracked(programApplicationKey);
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (suppressChangeEvents || rowIndex < 0 || rowIndex >= programApplicationKeys.size()) {
                return;
            }
            String programApplicationKey = programApplicationKeys.get(rowIndex);
            if (columnIndex == 1 && value instanceof String categoryId) {
                userSettings.setApplicationCategoryId(programApplicationKey, categoryId);
                persistAndNotify();
                fireTableRowsUpdated(rowIndex, rowIndex);
                return;
            }
            if (columnIndex == 2 && value instanceof Boolean tracked) {
                userSettings.setApplicationTracked(programApplicationKey, tracked);
                persistAndNotify();
                fireTableRowsUpdated(rowIndex, rowIndex);
            }
        }
    }

    private static final class CategoryItem {
        private final String categoryId;

        private CategoryItem(String categoryId) {
            this.categoryId = categoryId;
        }

        private String categoryId() {
            return categoryId;
        }

        @Override
        public String toString() {
            return ProgramCategoryDisplayNames.resolveDisplayName(categoryId);
        }
    }

    private static final class CategoryComboBoxRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof CategoryItem categoryItem) {
                label.setText(ProgramCategoryDisplayNames.resolveDisplayName(categoryItem.categoryId()));
            }
            return label;
        }
    }

    private static final class CategoryCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JComboBox<CategoryItem> categoryComboBox;
        private boolean adjustingSelection;

        private CategoryCellEditor(JComboBox<CategoryItem> categoryComboBox) {
            this.categoryComboBox = categoryComboBox;
            categoryComboBox.addActionListener(actionEvent -> {
                if (!adjustingSelection) {
                    stopCellEditing();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column
        ) {
            String categoryId = Objects.nonNull(value) ? String.valueOf(value) : ProgramCategoryIds.WORK;
            adjustingSelection = true;
            try {
                IntStream.range(0, categoryComboBox.getItemCount())
                        .filter(index -> categoryComboBox.getItemAt(index).categoryId().equalsIgnoreCase(categoryId))
                        .findFirst()
                        .ifPresentOrElse(
                                categoryComboBox::setSelectedIndex,
                                () -> {
                                    if (categoryComboBox.getItemCount() > 0) {
                                        categoryComboBox.setSelectedIndex(0);
                                    }
                                }
                        );
            } finally {
                adjustingSelection = false;
            }
            return categoryComboBox;
        }

        @Override
        public Object getCellEditorValue() {
            Object selectedItem = categoryComboBox.getSelectedItem();
            if (selectedItem instanceof CategoryItem categoryItem) {
                return categoryItem.categoryId();
            }
            return ProgramCategoryIds.WORK;
        }
    }

    private static final class CategoryCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column
            );
            String categoryId = Objects.nonNull(value) ? String.valueOf(value) : ProgramCategoryIds.WORK;
            label.setText(ProgramCategoryDisplayNames.resolveDisplayName(categoryId));
            return label;
        }
    }

    private static final class CategoryListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof String categoryId) {
                label.setText(ProgramCategoryDisplayNames.resolveDisplayName(categoryId));
            }
            return label;
        }
    }

    private static final class TrackToggleCellRenderer implements TableCellRenderer {
        private final JCheckBox trackToggle = new JCheckBox();

        private TrackToggleCellRenderer() {
            UiTheme.styleToggleSwitch(trackToggle);
            trackToggle.setHorizontalAlignment(SwingConstants.CENTER);
            trackToggle.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            trackToggle.setSelected(value instanceof Boolean tracked && tracked);
            trackToggle.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return trackToggle;
        }
    }

    private static final class TrackToggleCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JCheckBox trackToggle = new JCheckBox();

        private TrackToggleCellEditor() {
            UiTheme.styleToggleSwitch(trackToggle);
            trackToggle.setHorizontalAlignment(SwingConstants.CENTER);
            trackToggle.setOpaque(true);
            trackToggle.addActionListener(actionEvent -> stopCellEditing());
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column
        ) {
            trackToggle.setSelected(value instanceof Boolean tracked && tracked);
            trackToggle.setBackground(table.getSelectionBackground());
            return trackToggle;
        }

        @Override
        public Object getCellEditorValue() {
            return trackToggle.isSelected();
        }
    }
}
