package searchengine.services.management;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.RecursiveTask;
import java.util.logging.Logger;

public class NodePageExplorer extends RecursiveTask<TreeSet<String>> {
    private final Node node;
    private String mainUrl;
    private String url;
    private static final int SLEEP_TIME = 350;//in ms
    private static final Logger log = Logger.getLogger(NodePageExplorer.class.getName());


    public NodePageExplorer(Node node) {
        this.node = node;
    }

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
            throw new RuntimeException(e);
        }

        for (NodePageExplorer task : taskList) {
            TreeSet<String> tmpPages = task.join();
            log.info("Finished thread. Total size: " + tmpPages.size());
        }

        log.info("returning: " + Node.pages.size());
        return Node.pages;
    }


}
