import javax.swing.DefaultListModel;
import javax.swing.table.DefaultTableModel;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles saving and loading application data to/from a JSON file.
 */
class DataStore {
    private static final Path DATA_FILE = Paths.get(
            System.getProperty("user.home"), ".assignmenttracker_data.json");

    private DataStore() { }

    /** Saves all subjects and their assignments to disk. */
    static void save(DefaultListModel<Subject> model) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < model.size(); i++) {
            Subject s = model.get(i);
            sb.append("  {\n");
            sb.append("    \"name\": ").append(jsonString(s.getName())).append(",\n");
            sb.append("    \"assignments\": [\n");
            DefaultTableModel tm = s.getTableModel();
            for (int r = 0; r < tm.getRowCount(); r++) {
                String aName = (String) tm.getValueAt(r, 0);
                String date  = (String) tm.getValueAt(r, 1);
                Boolean done = (Boolean) tm.getValueAt(r, 2);
                sb.append("      {\"name\": ").append(jsonString(aName))
                  .append(", \"date\": ").append(jsonString(date))
                  .append(", \"done\": ").append(done != null && done)
                  .append("}");
                if (r < tm.getRowCount() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("    ]\n");
            sb.append("  }");
            if (i < model.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]\n");
        try {
            Files.write(DATA_FILE, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Could not save data: " + e.getMessage());
        }
    }

    /** Loads subjects and assignments from disk into the given model. */
    static void load(DefaultListModel<Subject> model) {
        if (!Files.exists(DATA_FILE)) return;
        try {
            String json = new String(Files.readAllBytes(DATA_FILE), StandardCharsets.UTF_8).trim();
            List<Subject> subjects = parseSubjects(json);
            for (Subject s : subjects) model.addElement(s);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Could not load data: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Minimal hand-rolled JSON parser (no external dependencies)
    // -----------------------------------------------------------------------

    private static List<Subject> parseSubjects(String json) {
        List<Subject> result = new ArrayList<>();
        json = json.trim();
        if (!json.startsWith("[")) return result;
        // Strip outer array brackets
        json = json.substring(1, json.length() - 1).trim();
        List<String> objects = splitTopLevelObjects(json);
        for (String obj : objects) {
            String name = extractStringField(obj, "name");
            Subject s = new Subject(name);
            String assignmentsBlock = extractArrayBlock(obj, "assignments");
            if (assignmentsBlock != null) {
                List<String> rows = splitTopLevelObjects(assignmentsBlock);
                for (String row : rows) {
                    String aName = extractStringField(row, "name");
                    String date  = extractStringField(row, "date");
                    boolean done = extractBoolField(row, "done");
                    s.getTableModel().addRow(new Object[]{aName, date, done});
                }
            }
            result.add(s);
        }
        return result;
    }

    /** Splits a JSON array body into individual {...} object strings. */
    private static List<String> splitTopLevelObjects(String body) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    parts.add(body.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return parts;
    }

    private static String extractStringField(String obj, String key) {
        String search = "\"" + key + "\"";
        int idx = obj.indexOf(search);
        if (idx < 0) return "";
        idx += search.length();
        while (idx < obj.length() && (obj.charAt(idx) == ' ' || obj.charAt(idx) == ':')) idx++;
        if (idx >= obj.length() || obj.charAt(idx) != '"') return "";
        idx++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (idx < obj.length()) {
            char c = obj.charAt(idx);
            if (c == '\\' && idx + 1 < obj.length()) {
                idx++;
                char esc = obj.charAt(idx);
                switch (esc) {
                    case '"':  sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    default:   sb.append(esc);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
            idx++;
        }
        return sb.toString();
    }

    private static boolean extractBoolField(String obj, String key) {
        String search = "\"" + key + "\"";
        int idx = obj.indexOf(search);
        if (idx < 0) return false;
        idx += search.length();
        while (idx < obj.length() && (obj.charAt(idx) == ' ' || obj.charAt(idx) == ':')) idx++;
        return obj.startsWith("true", idx);
    }

    private static String extractArrayBlock(String obj, String key) {
        String search = "\"" + key + "\"";
        int idx = obj.indexOf(search);
        if (idx < 0) return null;
        idx += search.length();
        while (idx < obj.length() && obj.charAt(idx) != '[') idx++;
        if (idx >= obj.length()) return null;
        int depth = 0;
        int start = idx;
        for (int i = idx; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return obj.substring(start + 1, i);
            }
        }
        return null;
    }

    private static String jsonString(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
