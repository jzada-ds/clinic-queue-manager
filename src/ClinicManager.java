public class ClinicManager {
    public static final String MIN_ID = "";
    public static final String MAX_ID = "\uFFFF\uFFFF\uFFFF\uFFFF";

    TwoThreeTree<String, DoctorTreeData, Void> doctors;
    TwoThreeTree<DoctorLoadTreeKey, DoctorLoadTreeKey, LoadAgg> doctorsLoadTree;
    TwoThreeTree<String, PatientTreeData, Void> patients;
    int priority;

    public ClinicManager() {
        priority = 0;
        doctors = newDoctorsTree();
        doctorsLoadTree = newDoctorsLoadTree();
        patients = newPatientsTree();
    }

    public void doctorEnter(String doctorId) {
        if(doctorId == null || doctors.Search(doctorId) != null)
            throw new IllegalArgumentException("");

        DoctorLoadTreeKey dKey = new DoctorLoadTreeKey(0, doctorId);
        TwoThreeTree<DoctorLoadTreeKey, DoctorLoadTreeKey, LoadAgg>.TTV loadNode = doctorsLoadTree.Insert(dKey, dKey);
        TwoThreeTree<Integer, QueueToDoctorData, Void> doctorQueue = newDoctorQueue();

        DoctorTreeData data = new DoctorTreeData(0, null, loadNode, doctorQueue);
        doctors.Insert(doctorId, data);
    }

    public void doctorLeave(String doctorId) {
        if(doctorId == null)
            throw new IllegalArgumentException("");

        TwoThreeTree<String, DoctorTreeData, Void>.TTV v =  doctors.Search(doctorId);
        if (v != null){
            if (v.Value().queueToDoctor.IsEmpty()){
                doctors.Delete(v);
                DoctorLoadTreeKey k = new DoctorLoadTreeKey(0, doctorId);
                doctorsLoadTree.Delete(doctorsLoadTree.Search(k));
            }else {
                throw new IllegalArgumentException();
            }
        }else{
            throw new IllegalArgumentException();
        }
    }

    public void patientEnter(String doctorId, String patientId) {
        if(doctorId == null || patientId == null)
            throw new IllegalArgumentException("");

        TwoThreeTree<String, DoctorTreeData, Void>.TTV doctor =  doctors.Search(doctorId);
        TwoThreeTree<String, PatientTreeData, Void>.TTV patient =  patients.Search(patientId);

        if (doctor == null || patient != null)
            throw new IllegalArgumentException();

        PatientTreeData data = new PatientTreeData(doctorId, ++priority);
        patient = patients.Insert(patientId, data);

        QueueToDoctorData queueData = new QueueToDoctorData(doctorId, patient);
        doctor.Value().queueToDoctor.Insert(priority, queueData);

        int newLoad = ++doctor.Value().patientCount;
        updateDoctorLoad(doctor, newLoad);
        doctor.Value().nextPatient = getNextPatient(doctor);
    }

    private void updateDoctorLoad(TwoThreeTree<String, DoctorTreeData, Void>.TTV doctor, int newLoad){
        TwoThreeTree<DoctorLoadTreeKey, DoctorLoadTreeKey, LoadAgg>.TTV dLoad = doctor.Value().doctorLoadNode;
        doctorsLoadTree.Delete(dLoad);
        DoctorLoadTreeKey newKey = new DoctorLoadTreeKey(newLoad, doctor.Key());
        doctor.Value().doctorLoadNode = doctorsLoadTree.Insert(newKey, newKey);
    }

    private String getNextPatient(TwoThreeTree<String, DoctorTreeData, Void>.TTV doctor) {
        TwoThreeTree<Integer, QueueToDoctorData, Void> q = doctor.Value().queueToDoctor;
        if (q == null || q.IsEmpty()) return null;

        TwoThreeTree<Integer, QueueToDoctorData, Void>.TTV t = q.Min();
        if (t == null || t.Value() == null || t.Value().patientTree == null) return null;

        return t.Value().patientTree.Key();
    }


    public String nextPatientLeave(String doctorId) {
        if(doctorId == null)
            throw new IllegalArgumentException("");

        TwoThreeTree<String, DoctorTreeData, Void>.TTV doctor = doctors.Search(doctorId);

        if (doctor == null || doctor.Value().queueToDoctor.IsEmpty())
            throw new IllegalArgumentException();

        String nextPatientID = getNextPatient(doctor);
        TwoThreeTree<Integer, QueueToDoctorData, Void> queueToDoctor = doctor.Value().queueToDoctor;
        TwoThreeTree<Integer, QueueToDoctorData, Void>.TTV patientInQueue = queueToDoctor.Min();
        TwoThreeTree<String, PatientTreeData, Void>.TTV nextPatient = patientInQueue.Value().patientTree;

        queueToDoctor.Delete(patientInQueue);
        patients.Delete(nextPatient);

        int newLoad = --doctor.Value().patientCount;
        updateDoctorLoad(doctor, newLoad);
        doctor.Value().nextPatient = getNextPatient(doctor);

        return nextPatientID;
    }

    public void patientLeaveEarly(String patientId) {
        if(patientId == null)
            throw new IllegalArgumentException("");

        TwoThreeTree<String, PatientTreeData, Void>.TTV patient = patients.Search(patientId);

        if (patient == null)
            throw new IllegalArgumentException();

        TwoThreeTree<String, DoctorTreeData, Void>.TTV doctor =  doctors.Search(patient.Value().doctorId);
        TwoThreeTree<Integer, QueueToDoctorData, Void> queueToDoctor = doctor.Value().queueToDoctor;

        queueToDoctor.Delete(queueToDoctor.Search(patient.Value().priority));
        patients.Delete(patient);

        int newLoad = --doctor.Value().patientCount;
        updateDoctorLoad(doctor, newLoad);
        doctor.Value().nextPatient = getNextPatient(doctor);
    }

    public int numPatients(String doctorId) {
        if(doctorId == null)
            throw new IllegalArgumentException("");

        TwoThreeTree<String, DoctorTreeData, Void>.TTV doctor = doctors.Search(doctorId);

        if (doctor == null)
            throw new IllegalArgumentException();

        return doctor.Value().patientCount;
    }

    public String nextPatient(String doctorId) {
        if(doctorId == null)
            throw new IllegalArgumentException("");

        TwoThreeTree<String, DoctorTreeData, Void>.TTV doctor = doctors.Search(doctorId);

        if (doctor == null || doctor.Value().queueToDoctor.IsEmpty())
            throw new IllegalArgumentException();

        return doctor.Value().nextPatient;
    }

    public String waitingForDoctor(String patientId) {
        if(patientId == null)
            throw new IllegalArgumentException("");

        TwoThreeTree<String, PatientTreeData, Void>.TTV patient = patients.Search(patientId);

        if (patient == null)
            throw new IllegalArgumentException();

        return patient.Value().doctorId;
    }

    private LoadAgg LoadRangeAgg(int a, int b) {
        if (a > b) throw new IllegalArgumentException();

        DoctorLoadTreeKey rightKey = new DoctorLoadTreeKey(b, MAX_ID);
        LoadAgg top = doctorsLoadTree.UpToKeyAgg(rightKey);

        LoadAgg bottom;
        if (a == Integer.MIN_VALUE) {
            bottom = new LoadAgg(0, 0);
        } else {
            DoctorLoadTreeKey leftKey = new DoctorLoadTreeKey(a - 1, MAX_ID);
            bottom = doctorsLoadTree.UpToKeyAgg(leftKey);
        }

        return new LoadAgg(top.doctorCount - bottom.doctorCount, top.loadSum - bottom.loadSum);
    }
    public int numDoctorsWithLoadInRange(int low, int high) {
        LoadAgg s = LoadRangeAgg(low, high);
        return s.doctorCount;
    }

    public int averageLoadWithinRange(int low, int high) {
        LoadAgg s = LoadRangeAgg(low, high);
        if (s.doctorCount <= 0) return 0;
        return s.loadSum / s.doctorCount;
    }

    //################################################ Trees ################################################


    //---------------------------- Load Tree -------------------------------
    private TwoThreeTree<DoctorLoadTreeKey, DoctorLoadTreeKey, LoadAgg> newDoctorsLoadTree(){
        return new TwoThreeTree<>(
                new DoctorLoadTreeKey(Integer.MIN_VALUE, MIN_ID),
                new DoctorLoadTreeKey(Integer.MAX_VALUE, MAX_ID),
                new TwoThreeTree.TTData<DoctorLoadTreeKey, LoadAgg>() {
                    @Override public LoadAgg empty() {
                        return new LoadAgg(0, 0);
                    }

                    @Override public LoadAgg agg(DoctorLoadTreeKey value) {
                        return new LoadAgg(1, value.patientCount);
                    }

                    @Override public LoadAgg combine(LoadAgg left, LoadAgg mid, LoadAgg right) {
                        int c = 0, s = 0;
                        if (left  != null) { c += left.doctorCount;  s += left.loadSum;  }
                        if (mid   != null) { c += mid.doctorCount;   s += mid.loadSum;   }
                        if (right != null) { c += right.doctorCount; s += right.loadSum; }
                        return new LoadAgg(c, s);
                    }
                }
        );
    }

    static private class DoctorLoadTreeKey implements Comparable<DoctorLoadTreeKey>{
        int patientCount;
        String doctorId;

        @Override
        public int compareTo(DoctorLoadTreeKey o) {
            int c = Integer.compare(this.patientCount, o.patientCount);
            if (c != 0) {return c; }
            return this.doctorId.compareTo(o.doctorId);
        }

        DoctorLoadTreeKey(int patientCount,String doctorId){
            this.patientCount = patientCount;
            this.doctorId = doctorId;
        }
    }

    static private class LoadAgg {
        int doctorCount;
        int loadSum;

        LoadAgg(int doctorCount, int loadSum) {
            this.doctorCount = doctorCount;
            this.loadSum = loadSum;
        }
    }


    //---------------------------- Queue to Doctor Tree -------------------------------

    private TwoThreeTree<Integer, QueueToDoctorData, Void> newDoctorQueue(){
        return new TwoThreeTree<>(0, Integer.MAX_VALUE, new TwoThreeTree.TTData<QueueToDoctorData, Void>() {
            @Override public Void empty() { return null;}
            @Override public Void agg(QueueToDoctorData value) {return null;}
            @Override public Void combine(Void left, Void mid, Void right) { return null;}
        });
    }

    static private class QueueToDoctorData {
        String doctorId;
        TwoThreeTree<String, PatientTreeData, Void>.TTV patientTree;

        QueueToDoctorData(String doctorId, TwoThreeTree<String, PatientTreeData, Void>.TTV patientTree){
            this.doctorId = doctorId;
            this.patientTree = patientTree;
        }
    }

    //---------------------------- Doctor Tree -------------------------------
    private TwoThreeTree<String, DoctorTreeData, Void> newDoctorsTree(){
        return new TwoThreeTree<>(MIN_ID, MAX_ID, new TwoThreeTree.TTData<DoctorTreeData, Void>() {
            @Override public Void empty() { return null; }
            @Override public Void agg(DoctorTreeData value) {return null; }
            @Override public Void combine(Void left, Void mid, Void right) { return null; }
        });
    }

    static private class DoctorTreeData {
        int patientCount;
        String nextPatient;
        TwoThreeTree<DoctorLoadTreeKey, DoctorLoadTreeKey, LoadAgg>.TTV doctorLoadNode;
        TwoThreeTree<Integer, QueueToDoctorData, Void> queueToDoctor;

        DoctorTreeData(int patientCount,
            String nextPatient,
            TwoThreeTree<DoctorLoadTreeKey, DoctorLoadTreeKey, LoadAgg>.TTV doctorLoadNode,
            TwoThreeTree<Integer, QueueToDoctorData, Void> queueToDoctor)
        {
            this.patientCount = patientCount;
            this.nextPatient = nextPatient;
            this.doctorLoadNode = doctorLoadNode;
            this.queueToDoctor = queueToDoctor;
        }
    }
    //---------------------------- Patient Tree -------------------------------
    private TwoThreeTree<String, PatientTreeData, Void> newPatientsTree(){
        return new TwoThreeTree<>(MIN_ID, MAX_ID, new TwoThreeTree.TTData<PatientTreeData, Void>() {
            @Override public Void empty() { return null; }
            @Override public Void agg(PatientTreeData value) { return null; }
            @Override public Void combine(Void left, Void mid, Void right) { return null; }
        });
    }

    static private class PatientTreeData {
        String doctorId;
        int priority;

        PatientTreeData(String doctorId, int priority){
            this.doctorId = doctorId;
            this.priority = priority;
        }
    }
}