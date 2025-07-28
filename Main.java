//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        Test<Integer, Integer> t = new Test();
        t.testInsertAndGetAllKeys();
        t.testDeletionScenarios();
        t.testDuplicateInsertion();
        t.testMergeAfterDeletions();
        t.testRedistribution();
        t.testStressAndOrder();
        t.testPersistenceCorrectness();
        System.out.println("All Tests Completed");
    }
}