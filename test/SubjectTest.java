import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import javax.swing.table.DefaultTableModel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link Subject} class.
 */
public class SubjectTest {

    private Subject subject;

    @BeforeEach
    void setUp() {
        subject = new Subject("Math");
    }

    // --- Constructor and basic getters ---

    @Test
    void constructorSetsName() {
        assertEquals("Math", subject.getName());
    }

    @Test
    void constructorCreatesEmptyTableModel() {
        DefaultTableModel model = subject.getTableModel();
        assertNotNull(model);
        assertEquals(0, model.getRowCount());
        assertEquals(4, model.getColumnCount());
    }

    @Test
    void tableModelHasCorrectColumnNames() {
        DefaultTableModel model = subject.getTableModel();
        assertEquals("Assignment", model.getColumnName(0));
        assertEquals("Due Date", model.getColumnName(1));
        assertEquals("Done", model.getColumnName(2));
        assertEquals("Notes", model.getColumnName(3));
    }

    @Test
    void tableModelColumnClassesAreCorrect() {
        DefaultTableModel model = subject.getTableModel();
        assertEquals(String.class, model.getColumnClass(0));
        assertEquals(String.class, model.getColumnClass(1));
        assertEquals(Boolean.class, model.getColumnClass(2));
        assertEquals(String.class, model.getColumnClass(3));
    }

    // --- setName ---

    @Test
    void setNameUpdatesName() {
        subject.setName("Science");
        assertEquals("Science", subject.getName());
    }

    // --- toString ---

    @Test
    void toStringReturnsName() {
        assertEquals("Math", subject.toString());
    }

    @Test
    void toStringReflectsRename() {
        subject.setName("Physics");
        assertEquals("Physics", subject.toString());
    }

    // --- Table model operations ---

    @Test
    void addRowIncreasesRowCount() {
        subject.getTableModel().addRow(new Object[]{"Homework 1", "2026-04-10", false});
        assertEquals(1, subject.getTableModel().getRowCount());
    }

    @Test
    void addedRowContainsCorrectData() {
        subject.getTableModel().addRow(new Object[]{"Homework 1", "2026-04-10", false});
        DefaultTableModel model = subject.getTableModel();
        assertEquals("Homework 1", model.getValueAt(0, 0));
        assertEquals("2026-04-10", model.getValueAt(0, 1));
        assertEquals(false, model.getValueAt(0, 2));
    }

    @Test
    void removeRowDecreasesRowCount() {
        DefaultTableModel model = subject.getTableModel();
        model.addRow(new Object[]{"HW1", "2026-04-10", false});
        model.addRow(new Object[]{"HW2", "2026-04-11", false});
        model.removeRow(0);
        assertEquals(1, model.getRowCount());
        assertEquals("HW2", model.getValueAt(0, 0));
    }

    @Test
    void multipleSubjectsHaveIndependentTableModels() {
        Subject other = new Subject("English");
        subject.getTableModel().addRow(new Object[]{"HW1", "2026-04-10", false});
        assertEquals(1, subject.getTableModel().getRowCount());
        assertEquals(0, other.getTableModel().getRowCount());
    }

    @Test
    void doneCheckboxCanBeToggled() {
        DefaultTableModel model = subject.getTableModel();
        model.addRow(new Object[]{"HW1", "2026-04-10", false});
        model.setValueAt(true, 0, 2);
        assertEquals(true, model.getValueAt(0, 2));
    }
}
