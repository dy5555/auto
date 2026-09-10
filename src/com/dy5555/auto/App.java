package com.dy5555.auto;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;
import java.util.List;

public class App {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { new MainFrame().setVisible(true); }
        });
    }
}

class MainFrame extends JFrame {
    MainFrame() {
        setTitle("업무 빠른 도구 - 프로시저 실행 현황");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1250, 720));
        setLocationRelativeTo(null);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("개발 DB", new ProcedureStatusPanel("DEV"));
        tabs.addTab("운영 DB", new ProcedureStatusPanel("PROD"));
        add(tabs, BorderLayout.CENTER);
    }
}

class ProcedureRecord {
    String status, procedureName, jobId, startTime, endTime, elapsedSec;
    String targetDate, processCount, errorCode, errorMessage;
}

class ProcedureTableModel extends AbstractTableModel {
    private final String[] columns = {"상태", "프로시저명", "작업ID", "시작시간", "종료시간", "수행시간(초)", "대상일자", "처리건수", "에러코드", "에러설명"};
    private final List<ProcedureRecord> rows = new ArrayList<ProcedureRecord>();
    void setRows(List<ProcedureRecord> newRows) { rows.clear(); rows.addAll(newRows); fireTableDataChanged(); }
    ProcedureRecord getRecord(int row) { return rows.get(row); }
    public int getRowCount() { return rows.size(); }
    public int getColumnCount() { return columns.length; }
    public String getColumnName(int column) { return columns[column]; }
    public Object getValueAt(int r, int c) {
        ProcedureRecord x = rows.get(r);
        switch (c) {
            case 0: return x.status; case 1: return x.procedureName; case 2: return x.jobId;
            case 3: return x.startTime; case 4: return x.endTime; case 5: return x.elapsedSec;
            case 6: return x.targetDate; case 7: return x.processCount; case 8: return x.errorCode;
            case 9: return x.errorMessage; default: return "";
        }
    }
}

class StatusCellRenderer extends DefaultTableCellRenderer {
    private static final Color ERROR_BG = new Color(255,225,225);
    private static final Color RUNNING_BG = new Color(255,247,204);
    private static final Color NORMAL_BG = new Color(228,247,232);
    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
        String status = String.valueOf(table.getValueAt(row, 0));
        if (!selected) {
            if ("오류".equals(status)) c.setBackground(ERROR_BG);
            else if ("진행중".equals(status)) c.setBackground(RUNNING_BG);
            else c.setBackground(NORMAL_BG);
        }
        setHorizontalAlignment(column == 0 ? CENTER : LEFT);
        setFont(getFont().deriveFont(column == 0 ? Font.BOLD : Font.PLAIN));
        setToolTipText(column == 9 && value != null ? value.toString() : null);
        return c;
    }
}

class DbConfig {
    private final Properties p = new Properties();
    DbConfig() throws Exception {
        InputStream in = new FileInputStream("config/app.properties");
        try { p.load(in); } finally { in.close(); }
    }
    String get(String key) { String v = p.getProperty(key); return v == null ? "" : v.trim(); }
}

class ProcedureDao {
    private final DbConfig cfg;
    ProcedureDao(DbConfig cfg) { this.cfg = cfg; }
    List<ProcedureRecord> find(String env, Date start, Date endExclusive) throws Exception {
        String prefix = "DEV".equals(env) ? "dev." : "prod.";
        String url = cfg.get(prefix + "url"), user = cfg.get(prefix + "user"), password = cfg.get(prefix + "password");
        String sql = cfg.get("query.procedureStatus");
        if (url.length() == 0 || user.length() == 0 || sql.length() == 0) throw new IllegalStateException(env + " DB 설정 또는 조회 SQL이 비어 있습니다.");
        List<ProcedureRecord> list = new ArrayList<ProcedureRecord>();
        try (Connection con = DriverManager.getConnection(url, user, password); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(start.getTime()));
            ps.setTimestamp(2, new Timestamp(endExclusive.getTime()));
            try (ResultSet rs = ps.executeQuery()) {
                Set<String> cols = columns(rs.getMetaData());
                while (rs.next()) {
                    ProcedureRecord r = new ProcedureRecord();
                    r.procedureName = value(rs, cols, "PROC_NAME"); r.jobId = value(rs, cols, "JOB_ID");
                    r.startTime = value(rs, cols, "START_TIME"); r.endTime = value(rs, cols, "END_TIME");
                    r.elapsedSec = value(rs, cols, "ELAPSED_SEC"); r.targetDate = value(rs, cols, "TARGET_DATE");
                    r.processCount = value(rs, cols, "PROCESS_COUNT"); r.errorCode = value(rs, cols, "ERROR_CODE");
                    r.errorMessage = value(rs, cols, "ERROR_MESSAGE"); r.status = decideStatus(value(rs, cols, "STATUS"), r);
                    list.add(r);
                }
            }
        }
        return list;
    }
    private Set<String> columns(ResultSetMetaData m) throws Exception {
        Set<String> s = new HashSet<String>(); for (int i=1; i<=m.getColumnCount(); i++) s.add(m.getColumnLabel(i).toUpperCase()); return s;
    }
    private String value(ResultSet rs, Set<String> cols, String name) throws Exception {
        if (!cols.contains(name)) return ""; Object v = rs.getObject(name); if (v == null) return "";
        if (v instanceof Timestamp) return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Timestamp)v); return String.valueOf(v);
    }
    private String decideStatus(String dbStatus, ProcedureRecord r) {
        String s = dbStatus == null ? "" : dbStatus.trim();
        if ("ERROR".equalsIgnoreCase(s) || "FAIL".equalsIgnoreCase(s) || "오류".equals(s)) return "오류";
        if ("RUNNING".equalsIgnoreCase(s) || "진행중".equals(s)) return "진행중";
        if ("SUCCESS".equalsIgnoreCase(s) || "NORMAL".equalsIgnoreCase(s) || "정상".equals(s)) return "정상";
        if (notEmpty(r.errorCode) || notEmpty(r.errorMessage)) return "오류";
        if (notEmpty(r.startTime) && !notEmpty(r.endTime)) return "진행중"; return "정상";
    }
    private boolean notEmpty(String s) { return s != null && s.trim().length() > 0; }
}

class ProcedureStatusPanel extends JPanel {
    private final String env;
    private final JTextField startField = new JTextField(10), endField = new JTextField(10);
    private final JButton searchButton = new JButton("조회");
    private final JLabel queryState = new JLabel("● 대기중");
    private final JLabel totalLabel = label("전체 0건"), normalLabel = label("정상 0건"), runningLabel = label("진행중 0건"), errorLabel = label("오류 0건");
    private final ProcedureTableModel model = new ProcedureTableModel();
    private final JTable table = new JTable(model);
    ProcedureStatusPanel(String env) {
        this.env = env; setLayout(new BorderLayout(10,10)); setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); startField.setText(today); endField.setText(today);
        add(top(), BorderLayout.NORTH); add(grid(), BorderLayout.CENTER);
        searchButton.addActionListener(e -> search());
        table.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) detail(model.getRecord(table.getSelectedRow())); } });
    }
    private static JLabel label(String s) { return new JLabel(s, SwingConstants.CENTER); }
    private JPanel top() {
        JPanel w = new JPanel(new BorderLayout(0,10)), p = new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        p.add(new JLabel("조회기간")); p.add(startField); p.add(new JLabel("~")); p.add(endField); p.add(searchButton); p.add(queryState);
        JPanel summary = new JPanel(new GridLayout(1,4,8,0)); JLabel[] ls={totalLabel,normalLabel,runningLabel,errorLabel};
        Color[] cs={new Color(235,238,242),new Color(228,247,232),new Color(255,247,204),new Color(255,225,225)};
        for(int i=0;i<ls.length;i++){ ls[i].setOpaque(true); ls[i].setBackground(cs[i]); ls[i].setFont(ls[i].getFont().deriveFont(Font.BOLD,15f)); ls[i].setBorder(BorderFactory.createEmptyBorder(8,8,8,8)); summary.add(ls[i]); }
        w.add(p,BorderLayout.NORTH); w.add(summary,BorderLayout.CENTER); return w;
    }
    private JScrollPane grid() {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); table.setRowHeight(26); table.setFillsViewportHeight(true); table.setDefaultRenderer(Object.class,new StatusCellRenderer());
        int[] widths={75,180,100,155,155,100,100,100,120,520}; for(int i=0;i<widths.length;i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]); return new JScrollPane(table);
    }
    private void search() {
        final Date start, endExclusive;
        try {
            SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd"); f.setLenient(false); start=f.parse(startField.getText().trim()); Date end=f.parse(endField.getText().trim());
            if(start.after(end)){ JOptionPane.showMessageDialog(this,"시작일이 종료일보다 늦습니다."); return; }
            Calendar c=Calendar.getInstance(); c.setTime(end); c.add(Calendar.DATE,1); endExclusive=c.getTime();
        } catch(Exception ex){ JOptionPane.showMessageDialog(this,"날짜는 yyyy-MM-dd 형식으로 입력해주세요."); return; }
        loading(true,"● DB 조회중...",new Color(180,120,0));
        new SwingWorker<List<ProcedureRecord>,Void>() {
            protected List<ProcedureRecord> doInBackground() throws Exception { return new ProcedureDao(new DbConfig()).find(env,start,endExclusive); }
            protected void done() {
                try { List<ProcedureRecord> rows=get(); model.setRows(rows); summary(rows); loading(false,"● 조회 완료",new Color(0,130,60)); }
                catch(Exception ex){ Throwable t=ex.getCause()!=null?ex.getCause():ex; loading(false,"● 조회 실패",Color.RED.darker()); JOptionPane.showMessageDialog(ProcedureStatusPanel.this,t.getMessage(),"DB 조회 오류",JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }
    private void summary(List<ProcedureRecord> rows) {
        int n=0,r=0,e=0; for(ProcedureRecord x:rows){ if("오류".equals(x.status))e++; else if("진행중".equals(x.status))r++; else n++; }
        totalLabel.setText("전체 "+rows.size()+"건"); normalLabel.setText("정상 "+n+"건"); runningLabel.setText("진행중 "+r+"건"); errorLabel.setText("오류 "+e+"건");
    }
    private void loading(boolean on,String text,Color color){ searchButton.setEnabled(!on); startField.setEnabled(!on); endField.setEnabled(!on); queryState.setText(text); queryState.setForeground(color); setCursor(on?Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR):Cursor.getDefaultCursor()); }
    private void detail(ProcedureRecord r) {
        JTextArea a=new JTextArea(); a.setEditable(false); a.setLineWrap(true); a.setWrapStyleWord(true); a.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
        a.setText("상태       : "+v(r.status)+"\n프로시저명 : "+v(r.procedureName)+"\n작업ID     : "+v(r.jobId)+"\n시작시간   : "+v(r.startTime)+"\n종료시간   : "+v(r.endTime)+"\n수행시간   : "+v(r.elapsedSec)+"\n대상일자   : "+v(r.targetDate)+"\n처리건수   : "+v(r.processCount)+"\n에러코드   : "+v(r.errorCode)+"\n\n[에러설명]\n"+v(r.errorMessage));
        JScrollPane sp=new JScrollPane(a); sp.setPreferredSize(new Dimension(800,420)); JOptionPane.showMessageDialog(this,sp,"상세 정보 - "+v(r.procedureName),JOptionPane.INFORMATION_MESSAGE);
    }
    private String v(String s){ return s==null?"":s; }
}
