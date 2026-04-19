package service;

import dao.BatchDAO;
import model.Batch;

import java.util.List;

public class BatchService {

    private BatchDAO batchDAO;

    public BatchService() {
        batchDAO = new BatchDAO();
    }

    public boolean addBatch(Batch batch) {
        return batchDAO.addBatch(batch);
    }

    public Batch getBatch(int batchId) {
        return batchDAO.getBatchById(batchId);
    }

    public List<Batch> getAllBatches() {
        return batchDAO.getAllBatches();
    }

    public boolean deleteBatch(int batchId) {
        return batchDAO.deleteBatch(batchId);
    }
}