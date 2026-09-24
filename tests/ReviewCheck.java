import java.util.*;
public class ReviewCheck {
  static int checks;
  static void eq(Object actual,Object expected) {
    checks++;
    if (!Objects.equals(actual,expected)) throw new AssertionError("Expected " + expected + ", got " + actual);
  }
  static void illegal(Runnable action) {
    checks++;
    try { action.run(); } catch (IllegalArgumentException e) { return; }
    throw new AssertionError("Expected IllegalArgumentException");
  }
  public static void main(String[] args) {
    for (int seed=0; seed<10; seed++) run(seed);
    System.out.println("PASS: 10 seeds x 3000 operations; " + checks + " reference-model checks.");
  }
  static void run(int seed) {
    ClinicManager cm=new ClinicManager();
    Map<String,ArrayList<String>> queues=new TreeMap<>();
    Map<String,String> owners=new TreeMap<>();
    Random r=new Random(seed);
    int serial=0;
    eq(cm.numDoctorsWithLoadInRange(0,100),0);
    eq(cm.averageLoadWithinRange(0,100),0);
    illegal(()->cm.doctorEnter(null));
    illegal(()->cm.patientLeaveEarly("absent"));
    for (int step=0;step<3000;step++) {
      String d="D"+r.nextInt(30);
      int op=r.nextInt(5);
      if (!queues.containsKey(d)) {
        cm.doctorEnter(d); queues.put(d,new ArrayList<>());
      } else {
        ArrayList<String> q=queues.get(d);
        if (op<=1) {
          String p="P"+(serial++); cm.patientEnter(d,p); q.add(p); owners.put(p,d);
        } else if(op==2 && !q.isEmpty()) {
          String p=q.remove(0); eq(cm.nextPatientLeave(d),p); owners.remove(p);
        } else if(op==3 && !owners.isEmpty()) {
          String p=new ArrayList<>(owners.keySet()).get(r.nextInt(owners.size()));
          cm.patientLeaveEarly(p); queues.get(owners.remove(p)).remove(p);
        } else if(op==4 && q.isEmpty()) {
          cm.doctorLeave(d); queues.remove(d);
        } else {
          illegal(()->cm.doctorEnter(d));
          if(!q.isEmpty()) illegal(()->cm.doctorLeave(d));
        }
      }
      for(var entry:queues.entrySet()) {
        String id=entry.getKey(); ArrayList<String> q=entry.getValue();
        eq(cm.numPatients(id),q.size());
        if(q.isEmpty()) illegal(()->cm.nextPatient(id));
        else eq(cm.nextPatient(id),q.get(0));
      }
      for(var entry:owners.entrySet()) eq(cm.waitingForDoctor(entry.getKey()),entry.getValue());
      int low=r.nextInt(20)-2, high=low+r.nextInt(20), count=0,sum=0;
      for(var q:queues.values()) if(q.size()>=low && q.size()<=high) { count++;sum+=q.size(); }
      eq(cm.numDoctorsWithLoadInRange(low,high),count);
      eq(cm.averageLoadWithinRange(low,high),count==0?0:sum/count);
      eq(cm.numDoctorsWithLoadInRange(Integer.MIN_VALUE,Integer.MAX_VALUE),queues.size());
    }
    for(String p:new ArrayList<>(owners.keySet())) cm.patientLeaveEarly(p);
    for(String d:queues.keySet()) { eq(cm.numPatients(d),0); cm.doctorLeave(d); }
    eq(cm.numDoctorsWithLoadInRange(0,100),0);
  }
}
