package gui;

import exceptions.DuplicateProductException;
import filehandler.FileHandler;
import models.*;
import threads.AutoSaveThread;
import threads.StockMonitorThread;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Optional;

public class GUIApp extends JFrame {

    private InventoryManager inventory   = new InventoryManager();
    private int              billCounter = 1;

    // Colors
    private static final Color BG_DARK   = new Color(18,  22,  30);
    private static final Color BG_PANEL  = new Color(28,  33,  45);
    private static final Color BG_CARD   = new Color(38,  44,  60);
    private static final Color ACCENT    = new Color(0,  200, 170);
    private static final Color ACCENT2   = new Color(255,120,  60);
    private static final Color TEXT_MAIN = new Color(220,230, 245);
    private static final Color TEXT_DIM  = new Color(120,135, 160);
    private static final Color RED_WARN  = new Color(255, 80,  80);

    // Fonts
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD,  18);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_BODY  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_MONO  = new Font("Consolas", Font.PLAIN, 12);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    // Components
    private JTable          productTable;
    private DefaultTableModel tableModel;
    private JTextArea       billArea;
    private JLabel          statusLabel;
    private JTextField      searchField;

    // Bill state
    private Customer currentCustomer;
    private Bill     currentBill;

    public GUIApp() {
        super("Inventory & Billing System");
        if (inventory.getAllProducts().isEmpty()) loadSampleData();
        initUI();
        startBackgroundThreads();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── Background Threads ────────────────────────────────────────────
    private void startBackgroundThreads() {
        // Method A: extends Thread
        AutoSaveThread autoSave = new AutoSaveThread(inventory, 30000);
        autoSave.start();

        // Method B: implements Runnable
        StockMonitorThread monitor = new StockMonitorThread(inventory, 3) {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(30000);
                        var low = inventory.getLowStockProducts(3);
                        SwingUtilities.invokeLater(() -> {
                            if (!low.isEmpty()) {
                                statusLabel.setText("  WARNING: LOW STOCK - " +
                                        low.get(0).getName() + " has only " +
                                        low.get(0).getQuantity() + " left!");
                                statusLabel.setForeground(RED_WARN);
                            }
                        });
                    } catch (InterruptedException e) { break; }
                }
            }
        };
        Thread t = new Thread(monitor, "StockMonitor");
        t.setDaemon(true);
        t.start();
    }

    // ── UI Setup ──────────────────────────────────────────────────────
    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);
        root.add(buildHeader(),    BorderLayout.NORTH);
        root.add(buildMain(),      BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(BG_PANEL);
        h.setBorder(new MatteBorder(0, 0, 1, 0, ACCENT));
        h.setPreferredSize(new Dimension(0, 60));
        JLabel title = new JLabel("   INVENTORY & BILLING SYSTEM");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(ACCENT);
        JLabel sub = new JLabel("Admin: Store Manager   ");
        sub.setFont(FONT_SMALL);
        sub.setForeground(TEXT_DIM);
        h.add(title, BorderLayout.WEST);
        h.add(sub,   BorderLayout.EAST);
        return h;
    }

    private JSplitPane buildMain() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildInventoryPanel(), buildBillingPanel());
        split.setDividerLocation(680);
        split.setBackground(BG_DARK);
        split.setBorder(null);
        return split;
    }

    // ── Inventory Panel ───────────────────────────────────────────────
    private JPanel buildInventoryPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(BG_DARK);
        p.setBorder(new EmptyBorder(12, 12, 12, 6));

        JLabel lbl = new JLabel("  INVENTORY");
        lbl.setFont(FONT_TITLE);
        lbl.setForeground(TEXT_MAIN);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setBackground(BG_DARK);
        searchField = styledField(16);
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filterTable(searchField.getText()); }
        });
        toolbar.add(makeLabel("Search:", TEXT_DIM));
        toolbar.add(searchField);
        toolbar.add(accentBtn("+ Add",    e -> showAddDialog()));
        toolbar.add(accentBtn("Edit",     e -> showEditDialog()));
        toolbar.add(dangerBtn("Remove",   e -> removeSelected()));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG_DARK);
        top.add(lbl,     BorderLayout.NORTH);
        top.add(toolbar, BorderLayout.SOUTH);

        p.add(top,                 BorderLayout.NORTH);
        p.add(buildProductTable(), BorderLayout.CENTER);
        return p;
    }

    private JScrollPane buildProductTable() {
        String[] cols = {"ID", "Name", "Category", "Price (Rs.)", "Stock", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        productTable = new JTable(tableModel);
        styleTable(productTable);
        refreshTable();
        JScrollPane sp = new JScrollPane(productTable);
        sp.getViewport().setBackground(BG_CARD);
        sp.setBorder(new LineBorder(BG_PANEL, 1));
        return sp;
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Product p : inventory.getAllProducts()) {
            String status = p.getQuantity() == 0 ? "OUT OF STOCK" :
                            p.getQuantity() <= 3  ? "LOW STOCK"   : "OK";
            tableModel.addRow(new Object[]{
                    p.getProductId(), p.getName(), p.getCategory(),
                    String.format("%.2f", p.getPrice()), p.getQuantity(), status
            });
        }
    }

    private void filterTable(String kw) {
        tableModel.setRowCount(0);
        for (Product p : inventory.searchByName(kw)) {
            String status = p.getQuantity() <= 3 ? "LOW STOCK" : "OK";
            tableModel.addRow(new Object[]{
                    p.getProductId(), p.getName(), p.getCategory(),
                    String.format("%.2f", p.getPrice()), p.getQuantity(), status
            });
        }
        if (kw.isEmpty()) refreshTable();
    }

    // ── Billing Panel ─────────────────────────────────────────────────
    private JPanel buildBillingPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(BG_DARK);
        p.setBorder(new EmptyBorder(12, 6, 12, 12));

        JLabel lbl = new JLabel("  BILLING");
        lbl.setFont(FONT_TITLE);
        lbl.setForeground(TEXT_MAIN);
        lbl.setBorder(new EmptyBorder(0, 0, 6, 0));

        billArea = new JTextArea();
        billArea.setFont(FONT_MONO);
        billArea.setBackground(new Color(12, 14, 20));
        billArea.setForeground(new Color(180, 255, 200));
        billArea.setEditable(false);
        billArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        billArea.setText("  No active bill.\n  Press 'New Bill' to start.");

        JScrollPane sp = new JScrollPane(billArea);
        sp.setBorder(new LineBorder(BG_PANEL));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        btns.setBackground(BG_DARK);
        btns.add(accentBtn("New Bill",    e -> startNewBill()));
        btns.add(accentBtn("Add Item",    e -> addItemToBill()));
        btns.add(styledBtn("Generate",   ACCENT2, e -> finalizeBill()));
        btns.add(styledBtn("View Bills", TEXT_DIM, e -> viewBillHistory()));

        p.add(lbl,  BorderLayout.NORTH);
        p.add(sp,   BorderLayout.CENTER);
        p.add(btns, BorderLayout.SOUTH);
        return p;
    }

    // ── Billing Actions ───────────────────────────────────────────────
    private void startNewBill() {
        JTextField nameField  = styledField(15);
        JTextField phoneField = styledField(12);
        JPanel form = makeForm(new String[]{"Customer Name:", "Phone:"},
                               new JTextField[]{nameField, phoneField});
        int res = JOptionPane.showConfirmDialog(this, form,
                "New Bill - Customer Details", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        String cname = nameField.getText().trim();
        if (cname.isEmpty()) { showError("Customer name is required."); return; }
        String custId = "C" + System.currentTimeMillis() % 10000;
        currentCustomer = new Customer(custId, cname, "n/a", phoneField.getText().trim());
        currentCustomer.register();
        String billId = String.format("B%04d", billCounter++);
        currentBill = new Bill(billId, currentCustomer);
        updateBillDisplay();
        setStatus("New bill started for " + cname);
    }

    private void addItemToBill() {
        if (currentBill == null) { showError("Start a new bill first."); return; }
        JTextField pidField = styledField(10);
        JTextField qtyField = styledField(6);
        JPanel form = makeForm(new String[]{"Product ID:", "Quantity:"},
                               new JTextField[]{pidField, qtyField});
        int res = JOptionPane.showConfirmDialog(this, form,
                "Add Item", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            String pid = pidField.getText().trim().toUpperCase();
            int qty = Integer.parseInt(qtyField.getText().trim());
            Optional<Product> opt = inventory.getProduct(pid);
            if (opt.isEmpty()) { showError("Product ID '" + pid + "' not found."); return; }
            Product p = opt.get();
            if (qty > p.getQuantity()) { showError("Only " + p.getQuantity() + " in stock."); return; }
            currentBill.addItem(p, qty);
            refreshTable();
            updateBillDisplay();
            setStatus("Added: " + p.getName() + " x" + qty);
        } catch (NumberFormatException e) { showError("Invalid quantity."); }
    }

    private void finalizeBill() {
        if (currentBill == null || currentBill.getItems().isEmpty()) {
            showError("No items in bill."); return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("=======================================================\n");
        sb.append("           INVENTORY & BILLING SYSTEM\n");
        sb.append("=======================================================\n");
        sb.append(String.format("Bill No : %s%n", currentBill.getBillId()));
        sb.append(String.format("Customer: %s  |  Phone: %s%n",
                currentCustomer.getName(), currentCustomer.getPhone()));
        sb.append("-------------------------------------------------------\n");
        sb.append(String.format("%-20s %4s %10s %12s%n","Item","Qty","Rate","Amount"));
        sb.append("-------------------------------------------------------\n");
        for (var item : currentBill.getItems()) {
            sb.append(String.format("%-20s %4d %10.2f %12.2f%n",
                    item.getProduct().getName(), item.getQuantity(),
                    item.getProduct().getPrice(), item.getSubtotal()));
        }
        sb.append("-------------------------------------------------------\n");
        sb.append(String.format("%-36s %12.2f%n", "Subtotal:", currentBill.getSubtotal()));
        sb.append(String.format("%-36s %12.2f%n", "GST (18%):", currentBill.getTax()));
        sb.append("=======================================================\n");
        sb.append(String.format("%-36s %12.2f%n", "TOTAL:", currentBill.calculateTotal()));
        sb.append("=======================================================\n");
        billArea.setText(sb.toString());

        FileHandler.saveBill(currentBill);
        inventory.save();
        setStatus("Bill " + currentBill.getBillId() + " generated. Total: Rs." +
                  String.format("%.2f", currentBill.calculateTotal()));
        currentBill = null;
        currentCustomer = null;
    }

    private void updateBillDisplay() {
        if (currentBill == null) { billArea.setText("  No active bill."); return; }
        StringBuilder sb = new StringBuilder();
        sb.append("  Bill: ").append(currentBill.getBillId())
          .append("  |  Customer: ").append(currentCustomer.getName()).append("\n");
        sb.append("  -----------------------------------------------\n");
        if (currentBill.getItems().isEmpty()) {
            sb.append("  (No items yet - click Add Item)\n");
        } else {
            for (var item : currentBill.getItems()) {
                sb.append(String.format("  %-20s x%-3d  Rs.%.2f%n",
                        item.getProduct().getName(), item.getQuantity(), item.getSubtotal()));
            }
            sb.append("  -----------------------------------------------\n");
            sb.append(String.format("  Subtotal : Rs.%.2f%n", currentBill.getSubtotal()));
            sb.append(String.format("  GST(18%%) : Rs.%.2f%n", currentBill.getTax()));
            sb.append(String.format("  TOTAL    : Rs.%.2f%n", currentBill.calculateTotal()));
        }
        billArea.setText(sb.toString());
    }

    private void viewBillHistory() {
        JTextArea area = new JTextArea(20, 55);
        area.setFont(FONT_MONO);
        area.setBackground(new Color(12, 14, 20));
        area.setForeground(new Color(180, 255, 200));
        area.setEditable(false);
        java.io.File f = new java.io.File("data/bills.txt");
        if (!f.exists()) {
            area.setText("No bills saved yet.");
        } else {
            try (var br = new java.io.BufferedReader(new java.io.FileReader(f))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 5)
                        sb.append(String.format("Bill: %-8s  Customer: %-8s  Total: Rs.%-10s  Status: %s%n",
                                parts[0], parts[1], parts[3], parts[4]));
                }
                area.setText(sb.toString());
            } catch (Exception ex) { area.setText("Error reading bills."); }
        }
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Bill History", JOptionPane.PLAIN_MESSAGE);
    }

    // ── Product Dialogs ───────────────────────────────────────────────
    private void showAddDialog() {
        JTextField nameField  = styledField(15);
        JTextField catField   = styledField(12);
        JTextField priceField = styledField(8);
        JTextField qtyField   = styledField(6);
        JPanel form = makeForm(
                new String[]{"Name:", "Category:", "Price (Rs.):", "Quantity:"},
                new JTextField[]{nameField, catField, priceField, qtyField});
        int res = JOptionPane.showConfirmDialog(this, form,
                "Add New Product", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            String name  = nameField.getText().trim();
            String cat   = catField.getText().trim();
            double price = Double.parseDouble(priceField.getText().trim());
            int    qty   = Integer.parseInt(qtyField.getText().trim());
            if (name.isEmpty()) { showError("Name cannot be empty."); return; }
            if (price <= 0)     { showError("Price must be greater than 0."); return; }
            String id = inventory.generateProductId();
            inventory.addProduct(new Product(id, name, cat, price, qty));
            refreshTable();
            setStatus("Product added: " + name + " [" + id + "]");
        } catch (DuplicateProductException e) { showError(e.getMessage()); }
          catch (NumberFormatException e)      { showError("Invalid price or quantity."); }
    }

    private void showEditDialog() {
        int row = productTable.getSelectedRow();
        if (row < 0) { showError("Please select a product from the table first."); return; }
        String id = (String) tableModel.getValueAt(row, 0);
        Optional<Product> opt = inventory.getProduct(id);
        if (opt.isEmpty()) return;
        Product p = opt.get();
        JTextField priceField = styledField(10);
        JTextField qtyField   = styledField(8);
        priceField.setText(String.valueOf(p.getPrice()));
        qtyField.setText(String.valueOf(p.getQuantity()));
        JPanel form = makeForm(new String[]{"Price (Rs.):", "New Quantity:"},
                               new JTextField[]{priceField, qtyField});
        int res = JOptionPane.showConfirmDialog(this, form,
                "Edit: " + p.getName(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            p.setPrice(Double.parseDouble(priceField.getText().trim()));
            p.setQuantity(Integer.parseInt(qtyField.getText().trim()));
            inventory.save();
            refreshTable();
            setStatus("Updated: " + p.getName());
        } catch (NumberFormatException e) { showError("Invalid value entered."); }
    }

    private void removeSelected() {
        int row = productTable.getSelectedRow();
        if (row < 0) { showError("Please select a product to remove."); return; }
        String id   = (String) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove '" + name + "'?",
                "Confirm Remove", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            inventory.removeProduct(id);
            refreshTable();
            setStatus("Removed: " + name);
        }
    }

    // ── Status Bar ────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        bar.setBackground(BG_PANEL);
        bar.setBorder(new MatteBorder(1, 0, 0, 0, BG_CARD));
        statusLabel = new JLabel("  System ready.");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(ACCENT);
        bar.add(statusLabel);
        return bar;
    }

    private void setStatus(String msg) {
        statusLabel.setForeground(ACCENT);
        statusLabel.setText("  " + msg);
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private void styleTable(JTable t) {
        t.setBackground(BG_CARD);
        t.setForeground(TEXT_MAIN);
        t.setFont(FONT_BODY);
        t.setRowHeight(28);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 2));
        t.setSelectionBackground(new Color(0, 140, 120));
        t.setSelectionForeground(Color.WHITE);
        t.getTableHeader().setBackground(BG_PANEL);
        t.getTableHeader().setForeground(TEXT_DIM);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
    }

    private JTextField styledField(int cols) {
        JTextField f = new JTextField(cols);
        f.setBackground(BG_CARD);
        f.setForeground(TEXT_MAIN);
        f.setCaretColor(ACCENT);
        f.setFont(FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BG_PANEL, 1), new EmptyBorder(4, 8, 4, 8)));
        return f;
    }

    private JButton accentBtn(String text, ActionListener a) {
        return styledBtn(text, ACCENT, a);
    }

    private JButton dangerBtn(String text, ActionListener a) {
        return styledBtn(text, RED_WARN, a);
    }

    private JButton styledBtn(String text, Color color, ActionListener a) {
        JButton b = new JButton(text);
        b.setFont(FONT_LABEL);
        b.setForeground(color);
        b.setBackground(BG_CARD);
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(color, 1), new EmptyBorder(5, 14, 5, 14)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(a);
        return b;
    }

    private JLabel makeLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(FONT_LABEL);
        return l;
    }

    private JPanel makeForm(String[] labels, JTextField[] fields) {
        JPanel p = new JPanel(new GridLayout(labels.length, 2, 8, 10));
        p.setBackground(BG_PANEL);
        p.setBorder(new EmptyBorder(14, 14, 14, 14));
        for (int i = 0; i < labels.length; i++) {
            JLabel l = new JLabel(labels[i]);
            l.setForeground(TEXT_MAIN);
            l.setFont(FONT_LABEL);
            p.add(l);
            p.add(fields[i]);
        }
        return p;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ── Sample Data ───────────────────────────────────────────────────
    private void loadSampleData() {
        try {
            inventory.addProduct(new Product("P001", "Basmati Rice (1kg)",  "Grocery",    85.00, 50));
            inventory.addProduct(new Product("P002", "Wheat Flour (5kg)",   "Grocery",   220.00, 30));
            inventory.addProduct(new Product("P003", "Sunflower Oil (1L)",  "Grocery",   150.00, 20));
            inventory.addProduct(new Product("P004", "Sugar (1kg)",         "Grocery",    50.00, 40));
            inventory.addProduct(new Product("P005", "Notebook (200pg)",    "Stationery", 60.00, 15));
            inventory.addProduct(new Product("P006", "Ball Pen (Pack/10)",  "Stationery", 45.00,  2));
            inventory.addProduct(new Product("P007", "Shampoo (200ml)",     "Personal",  120.00, 10));
            inventory.addProduct(new Product("P008", "Soap Bar (Pack/4)",   "Personal",   80.00,  8));
        } catch (DuplicateProductException ignored) {}
    }

    // ── Main ──────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUIApp::new);
    }
}
