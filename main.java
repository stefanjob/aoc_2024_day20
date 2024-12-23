import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class main {
    private static final int INF = Integer.MAX_VALUE;

    public static void main(String[] args) {
        
        boolean full = true;
        Scanner scanner = null;

        ArrayList<String> racetrack = new ArrayList<>();
 
        try {
            if (full) {
                scanner = new Scanner(new File("input_full.txt"));
            } else {
                scanner = new Scanner(new File("input_test.txt"));
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
 
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            System.out.println(line);
            racetrack.add(line);
        }
        Object[] gfg = racetrack.toArray();
        String[] str = Arrays.copyOf(gfg, gfg.length, String[].class);
        try {
            System.out.println("Cheats saving at least 100 picoseconds: " + countHighValueCheats(str));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int countHighValueCheats(String[] racetrack) throws InterruptedException, ExecutionException {
        int rows = racetrack.length;
        int cols = racetrack[0].length();
        char[][] grid = new char[rows][cols];

        // Parse grid and find start (S) and end (E) positions
        int[] start = new int[2], end = new int[2];
        List<int[]> walls = new ArrayList<>();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = racetrack[r].charAt(c);
                if (grid[r][c] == 'S') start = new int[]{r, c};
                if (grid[r][c] == 'E') end = new int[]{r, c};
                if (grid[r][c] == '#') walls.add(new int[]{r, c});
            }
        }

        // Compute the base distance without cheats
        int baseDistance = bfs(grid, start, end, false);

        // Multithreading: Split the work among threads
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        List<Future<Integer>> results = new ArrayList<>();
        int chunkSize = (walls.size() + numThreads - 1) / numThreads;

        final int[] startf = start;
        final int[] endf = end;

        for (int i = 0; i < walls.size(); i += chunkSize) {
            int fromIndex = i;
            int toIndex = Math.min(i + chunkSize, walls.size());

            // Submit task for a subset of walls
            results.add(executor.submit(() -> evaluateCheats(grid, startf, endf, walls, fromIndex, toIndex, baseDistance)));
        }

        // Collect results
        int totalCheats = 0;
        for (Future<Integer> result : results) {
            totalCheats += result.get();
        }

        executor.shutdown();
        return totalCheats;
    }

    private static int evaluateCheats(char[][] grid, int[] start, int[] end, List<int[]> walls,
                                      int fromIndex, int toIndex, int baseDistance) {
        int cheatsSaving100 = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            int[] startWall = walls.get(i);
            for (int j = 0; j < walls.size(); j++) {
                int[] endWall = walls.get(j);

                // Try cheating between startWall and endWall
                int cheatDistance = bfsWithCheat(grid, start, end, startWall[0], startWall[1], endWall[0], endWall[1]);
                if (cheatDistance == INF) continue;

                int timeSaved = baseDistance - cheatDistance;
                if (timeSaved >= 100) {
                    cheatsSaving100++;
                }
            }
        }

        return cheatsSaving100;
    }

    /*
    private static int countHighValueCheats(String[] racetrack) {
        int rows = racetrack.length;
        int cols = racetrack[0].length();
        char[][] grid = new char[rows][cols];

        // Parse grid and find start (S) and end (E) positions
        int[] start = new int[2], end = new int[2];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = racetrack[r].charAt(c);
                if (grid[r][c] == 'S') start = new int[]{r, c};
                if (grid[r][c] == 'E') end = new int[]{r, c};
            }
        }

        // Shortest path from start to end without cheating
        int baseDistance = bfs(grid, start, end, false);

        int cheatsSaving100 = 0;

        // Evaluate cheats: try cheating from every wall to every wall
        for (int r1 = 0; r1 < rows; r1++) {
            for (int c1 = 0; c1 < cols; c1++) {
                if (grid[r1][c1] != '#') continue;

                for (int r2 = 0; r2 < rows; r2++) {
                    for (int c2 = 0; c2 < cols; c2++) {
                        if (grid[r2][c2] != '#') continue;

                        // Try cheating between (r1, c1) and (r2, c2)
                        int cheatDistance = bfsWithCheat(grid, start, end, r1, c1, r2, c2);
                        if (cheatDistance == INF) continue;

                        int timeSaved = baseDistance - cheatDistance;
                        if (timeSaved >= 100) {
                            cheatsSaving100++;
                        }
                    }
                }
            }
        }

        return cheatsSaving100;
    }
    */

    private static int bfs(char[][] grid, int[] start, int[] end, boolean allowCheat) {
        int rows = grid.length, cols = grid[0].length;
        boolean[][] visited = new boolean[rows][cols];
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{start[0], start[1], 0}); // {row, col, distance}

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int r = current[0], c = current[1], dist = current[2];

            if (r == end[0] && c == end[1]) return dist;

            for (int[] dir : directions) {
                int nr = r + dir[0], nc = c + dir[1];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols &&
                    (grid[nr][nc] == '.' || (allowCheat && grid[nr][nc] == '#')) &&
                    !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    queue.offer(new int[]{nr, nc, dist + 1});
                }
            }
        }

        return INF;
    }

    private static int bfsWithCheat(char[][] grid, int[] start, int[] end, int cr1, int cc1, int cr2, int cc2) {
        int rows = grid.length, cols = grid[0].length;
        boolean[][] visited = new boolean[rows][cols];
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{start[0], start[1], 0}); // {row, col, distance}

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int r = current[0], c = current[1], dist = current[2];

            if (r == end[0] && c == end[1]) return dist;

            for (int[] dir : directions) {
                int nr = r + dir[0], nc = c + dir[1];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    boolean isWall = grid[nr][nc] == '#';
                    boolean canCheat = (r == cr1 && c == cc1) || (r == cr2 && c == cc2);
                    if ((grid[nr][nc] == '.' || canCheat) && !visited[nr][nc]) {
                        visited[nr][nc] = true;
                        queue.offer(new int[]{nr, nc, dist + 1});
                    }
                }
            }
        }

        return INF;
    }
}