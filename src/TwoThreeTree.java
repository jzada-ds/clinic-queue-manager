public class TwoThreeTree<K extends Comparable<K>, V, A> {

    private final TTData<V, A> dataCalc;
    private TTV root;
    private final K minID, maxID;
    private int size;

    public TwoThreeTree(K minID, K maxID, TTData<V, A> dataCalc) {
        this.size = 0;
        this.dataCalc = dataCalc;
        this.minID = minID;
        this.maxID = maxID;

        TTV min = new TTV(minID, null);
        TTV max = new TTV(maxID, null);
        root = new TTV();
        root.SetChildren(min, max, null);
    }

    public int Size(){
        return size;
    }

    public boolean IsEmpty(){
        return Size() == 0;
    }

    public TTV Search(K key) {
        return Search(root, key);
    }

    private TTV Search(TTV x, K key) {
        if (x == null) return null;

        if (x.isLeaf()) {
            return x.key.compareTo(key) == 0? x: null;
        }

        if (key.compareTo(x.left.key) <= 0) {
            return Search(x.left, key);
        } else if (key.compareTo(x.middle.key) <= 0) {
            return Search(x.middle, key);
        } else {
            return Search((x.right != null) ? x.right : x.middle, key);
        }
    }

    public TTV Min(){
        if(root.isLeaf()){
            throw new IllegalArgumentException("");
        }

        TTV x = root;
        while (!x.isLeaf())
            x = x.left;
        x = x.parent.middle;

        if (!x.key.equals(maxID))
            return x;
        throw new IllegalArgumentException("");
    }

    public TTV Successor(TTV x) {
        if (x == null || x.parent == null) return null;

        TTV z = x.parent;

        while (z != null && (x == z.right || (z.right == null && x == z.middle))) {
            x = z;
            z = z.parent;
        }

        if (z == null) return null;

        TTV y = (x == z.left) ? z.middle : z.right;

        while (y != null && !y.isLeaf()) {
            y = y.left;
        }

        if (y != null && y.key.compareTo(maxID) < 0) return y;
        return null;
    }


    private TTV InsertAndSplit(TTV parent, TTV child){
        TTV l = parent.left;
        TTV m = parent.middle;
        TTV r = parent.right;
        if (r == null){
            if (child.key.compareTo(l.key) <= 0)
                parent.SetChildren(child,l,m);
            else if (child.key.compareTo(m.key) <= 0)
                parent.SetChildren(l,child,m);
            else
                parent.SetChildren(l,m,child);
            return null;
        }
        TTV y = new TTV();

        if (child.key.compareTo(l.key) <= 0){
            parent.SetChildren(child, l, null);
            y.SetChildren(m, r, null);
        } else if (child.key.compareTo(m.key) <= 0) {
            parent.SetChildren(l, child, null);
            y.SetChildren(m, r, null);
        } else if (child.key.compareTo(r.key) <= 0) {
            parent.SetChildren(l, m, null);
            y.SetChildren(child, r, null);
        } else {
            parent.SetChildren(l, m, null);
            y.SetChildren(r, child, null);
        }
        return y;
    }


    public TTV Insert(K key, V value) {
        TTV v = new TTV(key, value);
        Insert(v);
        return v;
    }

    private void Insert(TTV z){
        TTV y = root;
        while (!y.isLeaf()) {
            if (z.key.compareTo(y.left.key) <= 0) {
                y = y.left;
            } else if (z.key.compareTo(y.middle.key) <= 0) {
                y = y.middle;
            } else {
                y = (y.right != null) ? y.right : y.middle;
            }
        }

        TTV x = y.parent;
        z = InsertAndSplit(x, z);
        while (x != root){
            x = x.parent;
            if (z != null)
                z = InsertAndSplit(x, z);
            else
                x.UpdateVertex();;
        }

        if(z != null){
            TTV w = new TTV();
            w.SetChildren(x,z, null);
            root = w;
        }
        size++;
    }

    private TTV BorrowOrMerge(TTV y){
        TTV z = y.parent;
        TTV x;
        if (y == z.left) {
            x = z.middle;
            if (x.right != null) {
                y.SetChildren(y.left, x.left, null);
                x.SetChildren(x.middle, x.right, null);
            } else {
                x.SetChildren(y.left, x.left, x.middle);
                z.SetChildren(x, z.right, null);
            }
            return z;
        }
        if (y == z.middle){
            x = z.left;
            if (x.right != null){
                y.SetChildren(x.right, y.left, null);
                x.SetChildren(x.left, x.middle, null);
            }
            else{
                x.SetChildren(x.left, x.middle, y.left);
                z.SetChildren(x, z.right, null);
            }
            return z;
        }

        x = z.middle;
        if (x.right != null){
            y.SetChildren(x.right, y.left, null);
            x.SetChildren(x.left, x.middle, null);
        }
        else{
            x.SetChildren(x.left, x.middle, y.left);
            z.SetChildren(z.left, x, null);
        }
        return z;
    }

    public void Delete(TTV x) {
        if (x == null || x.parent == null)
            throw new IllegalArgumentException("");
        TTV y = x.parent;
        if (x == y.left)
         y.SetChildren(y.middle, y.right, null);
        else if (x == y.middle)
            y.SetChildren(y.left, y.right, null);
        else
            y.SetChildren(y.left, y.middle, null);

        while (y != null){
            if (y.middle != null){
                y.UpdateVertex();
                y = y.parent;
            }

            else if (y != root){
                y = BorrowOrMerge(y);
            }
            else {
                root = y.left;
                y.left.parent = null;
                size--;
                return;
            }
        }
        size--;
    }

    public A UpToKeyAgg(K key) {
        if (root == null) return dataCalc.empty();
        if (key.compareTo(minID) <= 0) return dataCalc.empty();
        if (key.compareTo(root.key) >= 0) return root.agg;

        A agg = dataCalc.empty();
        TTV x = root;

        while (!x.isLeaf()) {
            if (key.compareTo(x.left.key) <= 0) {
                x = x.left;
                continue;
            }

            agg = dataCalc.combine(agg, x.left.agg, dataCalc.empty());
            if (key.compareTo(x.middle.key) <= 0) {
                x = x.middle;
                continue;
            }

            agg = dataCalc.combine(agg, x.middle.agg, dataCalc.empty());
            if (x.right == null) return agg;
            if (key.compareTo(x.right.key) <= 0) {
                x = x.right;
            } else {
                agg = dataCalc.combine(agg, x.right.agg, dataCalc.empty());
                return agg;
            }
        }

        if (x.key.compareTo(key) <= 0) {
            agg = dataCalc.combine(agg, x.agg, dataCalc.empty());
        }
        return agg;
    }

    public interface TTData<V, A> {
        A empty();
        A agg(V value);
        A combine(A left, A mid, A right);
    }

    public class TTV {
        TTV parent, left, middle, right;
        K key;
        V value;
        A agg;

        TTV() {}

        public V Value(){
            return value;
        }

        public K Key(){
            return key;
        }

        TTV(K key, V value) {
            this.key = key;
            this.value = value;
            this.agg = (value == null) ? dataCalc.empty() : dataCalc.agg(value);
        }

        boolean isLeaf(){
            return left == null && right == null && middle == null;
        }

        void SetChildren(TTV left, TTV middle, TTV right) {
            this.left = left; this.middle = middle; this.right = right;
            if (left != null) left.parent = this;
            if (middle != null) middle.parent = this;
            if (right != null) right.parent = this;
            UpdateVertex();
        }

        void UpdateVertex() {
            UpdateKey();
            UpdateAgg();
        }

        void UpdateKey() {
            this.key = left.key;
            if (middle != null) this.key = middle.key;
            if (right != null) this.key = right.key;
        }

        void UpdateAgg() {
            if (isLeaf()) {
                this.agg = (value == null) ? dataCalc.empty() : dataCalc.agg(value);
            } else {
                A l = (left   != null && left.agg   != null) ? left.agg   : dataCalc.empty();
                A m = (middle != null && middle.agg != null) ? middle.agg : dataCalc.empty();
                A r = (right  != null && right.agg  != null) ? right.agg  : dataCalc.empty();
                this.agg = dataCalc.combine(l, m, r);
            }
        }
    }
}
