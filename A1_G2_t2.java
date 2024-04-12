import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class FPAlgorithm {
    static class Node {
        Node parent;
        String item;
        int count;
        Node link;
        List<Node> children = new ArrayList<>();

        public Node(String item, int count, Node parent, Node link) {
            this.item = item;
            this.count = count;
            this.parent = parent;
            this.link = link;
        }

        public int getCount() {
            return count;
        }

        public List<Node> singlePath() {
            List<Node> t = new ArrayList<>();
            if (children.isEmpty()) {
                t.add(this);
                return t;
            }
            else if (children.size() == 1) {
                t.add(this);
                t.addAll(children.get(0).singlePath());
                return t;
            }
            else {
                t.add(null);
                return t;
            }
        }

        public void genCPB(List<String> pattern, int support, Map<List<String>, Integer> conditionalPatternBase) {
            pattern.add(0, item);
            support = Math.min(support, count);
            if (parent.item == null) {
                int finalSupport = support;
                conditionalPatternBase.compute(pattern, (key, val) -> val == null ? finalSupport : val + finalSupport);
            }
            else parent.genCPB(pattern, support, conditionalPatternBase);
        }
    }
    static double minimumSupportValue;
    static int row = 0;
    static Map<List<String>, Integer> answer = new HashMap<>();

    public static void main(String[] args) {
        long startTime = System.nanoTime();
        String path = args[0];
        minimumSupportValue = Double.parseDouble(args[1]);
        List<HashSet<String>> D = new ArrayList<>();

        String line = "";
        Map<String, Integer> counter = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            while ((line = br.readLine()) != null) {
                row++;
                HashSet<String> items = Arrays.stream(line.split(","))
                        .collect(Collectors.toCollection(HashSet::new));
                D.add(items);
                for (String item : items) {
                    counter.compute(item, (key, val) -> val == null ? 1 : val + 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> L = counter.entrySet().stream()
                .filter(entry -> (double) entry.getValue() / row >= minimumSupportValue)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey).toList();

        Node root = new Node(null, 0, null, null);
        Map<String, Node> headOfNodeLink = new HashMap<>();
        for (HashSet<String> T : D) {
            List<String> P = new ArrayList<>();
            for (String element : L) {
                if (T.contains(element)) {
                    P.add(element);
                }
            }
            if (!P.isEmpty())
                insert_tree(P.get(0), P.size() > 1 ? P.subList(1, P.size()) : null, root, headOfNodeLink);
        }

        fp_growth(root, null, headOfNodeLink);
        System.out.println("Execution time in milliseconds: " + (System.nanoTime() - startTime) / 1000000);
        // answer.entrySet().stream()
        //         .sorted(Comparator.comparingDouble(e -> (double) e.getValue() / finalRow))
        //         .forEach(e -> {
        //             String items = String.join(", ", e.getKey());
        //             double support = (double) e.getValue() / finalRow;
        //             System.out.printf("%s %f\n", items, support);
        //         });
        System.out.println(answer.size());
    }

    private static void insert_tree(String p, List<String> P, Node T, Map<String, Node> headOfNodeLink) {
        Node correctChild = T.children.stream().filter(entry -> entry.item.equals(p)).findFirst().orElse(null);
        if (correctChild != null){
            correctChild.count += 1;
            if (P != null)
                insert_tree(P.get(0), P.size() > 1 ? P.subList(1, P.size()) : null, correctChild, headOfNodeLink);
        }
        else {
            Node N = new Node(p, 1, T, headOfNodeLink.get(p));
            T.children.add(N);
            headOfNodeLink.put(p, N);
            if (P != null)
                insert_tree(P.get(0), P.size() > 1 ? P.subList(1, P.size()) : null, N, headOfNodeLink);
        }
    }
    private static void fp_growth(Node Tree, List<String> alpha, Map<String, Node> headOfNodeLink) {
        List<Node> singlePath = Tree.singlePath();
        if (!singlePath.contains(null)) {
            if (singlePath.get(0).item == null)
                singlePath.remove(0);
            int support = singlePath.get(0).count;
            genPattern(singlePath, alpha == null ? new ArrayList<>() : alpha, support, 0, new ArrayList<>());
        }
        else {
            for (Node alpha_i : headOfNodeLink.values()) {
                List<String> beta = new ArrayList<>();
                beta.add(alpha_i.item);
                if (alpha != null)
                    beta.addAll(alpha);

                Map<List<String>, Integer> conditionalPatternBase = new HashMap<>();
                do {
                    Node finalAlpha_i = alpha_i;
                    answer.compute(beta, (key, val) -> val == null ? finalAlpha_i.count : val + finalAlpha_i.count);
                    if (alpha_i.parent.item != null)
                        alpha_i.parent.genCPB(new ArrayList<>(), alpha_i.count, conditionalPatternBase);
                    alpha_i = alpha_i.link;
                } while (alpha_i != null);

                Map<String, Integer> counter = new HashMap<>();
                for (List<String> T : conditionalPatternBase.keySet()) {
                    int support = conditionalPatternBase.get(T);
                    for (String item : T)
                        counter.compute(item, (key, val) -> val == null ? support : val + support);
                }
                List<String> L = counter.entrySet().stream()
                        .filter(entry -> (double) entry.getValue() / row >= minimumSupportValue)
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .map(Map.Entry::getKey).toList();

                Node treeBeta = new Node(null, 0, null, null);
                Map<String, Node> newHeadOfNodeLink = new HashMap<>();
                for (Map.Entry<List<String>, Integer> T : conditionalPatternBase.entrySet()) {
                    List<String> P = new ArrayList<>();
                    for (String element : L) {
                        if (T.getKey().contains(element)) {
                            P.add(element);
                        }
                    }
                    if (!P.isEmpty())
                        insert_tree_pattern(P.get(0), P.size() > 1 ? P.subList(1, P.size()) : null, treeBeta, T.getValue(), newHeadOfNodeLink);

                }
                if (!treeBeta.children.isEmpty())
                    fp_growth(treeBeta, beta, newHeadOfNodeLink);
            }
        }
    }

    private static void genPattern(List<Node> beta, List<String> alpha, int support, int i, List<String> t) {
        if (!t.isEmpty()){
            List<String> in = new ArrayList<>(t);
            in.addAll(alpha);
            answer.compute(in, (key, val) -> val == null ? support : val + support);
        }
        for (; i < beta.size(); i++) {
            t.add(beta.get(i).item);
            genPattern(beta, alpha, Math.min(beta.get(i).count, support), i + 1, t);
            t.remove(t.size() - 1);
        }
    }

    private static void insert_tree_pattern(String p, List<String> P, Node T, int support, Map<String, Node> newHeadOfNodeLink) {
        Node correctChild = T.children.stream().filter(entry -> entry.item.equals(p)).findFirst().orElse(null);
        if (correctChild != null){
            correctChild.count += support;
            if (P != null)
                insert_tree_pattern(P.get(0), P.size() > 1 ? P.subList(1, P.size()) : null, correctChild, support, newHeadOfNodeLink);
        }
        else {
            Node N = new Node(p, support, T, newHeadOfNodeLink.get(p));
            T.children.add(N);
            newHeadOfNodeLink.put(p, N);
            if (P != null)
                insert_tree_pattern(P.get(0), P.size() > 1 ? P.subList(1, P.size()) : null, N, support, newHeadOfNodeLink);
        }
    }
}
