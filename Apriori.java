import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Apriori {

    public static void main(String[] args) {
        long startTime = System.nanoTime();
        Map<HashSet<String>, Integer> answer = new HashMap<>();
        String path = args[0];
        double minimumSupportValue = Double.parseDouble(args[1]);
        List<HashSet<String>> D = new ArrayList<>();
        int row = 0;
        Map<String, Integer> counter = new HashMap<>();

        String line = "";
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            while ((line = br.readLine()) != null) {
                row++;
                String[] items = line.split(",");
                D.add(Arrays.stream(items)
                        .collect(Collectors.toCollection(HashSet::new)));
                for (String item : items) {
                    counter.compute(item, (key, val) -> val == null ? 1 : val + 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        int finalRow = row;

        Map<HashSet<String>, Integer> Lkm1 = new HashMap<>(
                counter.entrySet().stream()
                        .filter(entry -> (double) entry.getValue() / finalRow >= minimumSupportValue)
                        .collect(Collectors.toMap(
                                entry -> new HashSet<>(Collections.singletonList(entry.getKey())),
                                Map.Entry::getValue
                        ))
        );

        for (int k = 2; !Lkm1.isEmpty(); k++) {
            answer.putAll(Lkm1);
            Map<HashSet<String>, Integer> Ck = aprioriGen(Lkm1);
            for (HashSet<String> t : D) {
                Map<HashSet<String>, Integer> Ct = subset(Ck, t);
                for (HashSet<String> c : Ct.keySet()) {
                    Ck.compute(c, (key, val) -> val + 1);
                }
            }

            Lkm1 = Ck.entrySet().stream()
                    .filter(entry -> (double) entry.getValue() / finalRow >= minimumSupportValue)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        System.out.println("Execution time in milliseconds: " + (System.nanoTime() - startTime) / 1000000);
        answer.entrySet().stream()
                .sorted(Comparator.comparingDouble(e -> (double) e.getValue() / finalRow))
                .forEach(e -> {
                    String items = String.join(", ", e.getKey());
                    double support = (double) e.getValue() / finalRow;
                    System.out.printf("%s %f\n", items, support);
                });
    }

    public static Map<HashSet<String>, Integer> aprioriGen(Map<HashSet<String>, Integer> Lkm1) {
        Map<HashSet<String>, Integer> Ck = new HashMap<>();
        for (HashSet<String> p : Lkm1.keySet())
            for (HashSet<String> q : Lkm1.keySet()) {
                HashSet<String> t1 = new HashSet<>(p);
                HashSet<String> t2 = new HashSet<>(p);
                t1.retainAll(q);
                if (t1.size() == p.size() - 1) {
                    t2.addAll(q);
                    Ck.put(t2, 0);
                }
            }
        Set<HashSet<String>> toRemove = new HashSet<>();
        for (HashSet<String> c: Ck.keySet()) {
            for (String one : c) {
                HashSet<String> s = new HashSet<>(c);
                s.remove(one);
                if (!Lkm1.containsKey(s))
                    toRemove.add(c);
            }
        }
        toRemove.forEach(Ck::remove);
        return Ck;
    }

    private static Map<HashSet<String>, Integer> subset(Map<HashSet<String>, Integer> Ct, HashSet<String> t) {
        Map<HashSet<String>, Integer> ret = new HashMap<>();
        for (HashSet<String> c: Ct.keySet())
            if (t.containsAll(c))
                ret.put(c, 0);
        return ret;
    }
}
