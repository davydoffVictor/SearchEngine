package searchengine.services.management;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.RecursiveTask;

@Slf4j
@RequiredArgsConstructor
public class NodePageExplorer extends RecursiveTask<TreeSet<String>> {
    private final Node node;
    private String mainUrl;
    private String url;
    private static final int SLEEP_TIME = 350;//in ms

    @Override
    protected TreeSet<String> compute() {
        List<NodePageExplorer> taskList = new ArrayList<>();
        try {
            for (Node child: node.getChildren()) {
                NodePageExplorer task = new NodePageExplorer(child);
                Thread.sleep(SLEEP_TIME);
                task.fork();
                taskList.add(task);
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }

        for (NodePageExplorer task : taskList) {
            TreeSet<String> tmpPages = task.join();
            log.info("Finished thread. Total size: " + tmpPages.size());
        }

        log.info("returning: " + Node.pages.size());
        return Node.pages;
    }
}
