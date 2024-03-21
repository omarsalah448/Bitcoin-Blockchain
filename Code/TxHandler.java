import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. 
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    public UTXOPool getUTXOPool() {
        return this.utxoPool;
    }
    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        double inputSum  = 0;
        double outputSum = 0;
        Set<UTXO> utxoSet = new HashSet<UTXO> ();
        // looping through the input values
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            // (1) check if not in UTXO Pool
            if (!utxoPool.contains(utxo))
                return false;
            Transaction.Output prevOutput = utxoPool.getTxOutput(utxo);
            // (2) check if signature on each input is valid
            if (!Crypto.verifySignature(prevOutput.address, tx.getRawDataToSign(i), input.signature))
                return false;
            // (3) check if a UTXO is claimed before
            if (utxoSet.contains(utxo))
                return false;
            utxoSet.add(utxo);
            inputSum += prevOutput.value;
        }
        // looping through the output values
        for (int i = 0; i < tx.numOutputs(); i++) {
            Transaction.Output output = tx.getOutput(i);
            // (4) check if output value is negative
            if (output.value < 0)
                return false;
            outputSum += output.value;
        }
        // (5) check if input sum is smaller than output sum
        if (inputSum < outputSum)
            return false;
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> validTransactions = new ArrayList<Transaction>();
        for (int i = 0; i < possibleTxs.length; i++) {
            for (Transaction tx : possibleTxs) {
                // if transaction is valid and not taken before, then add it
                if (isValidTx(tx) && !validTransactions.contains(tx)) {
                    validTransactions.add(tx);
                    // we need to add the outputs to the UTXO Pool
                    for (int outIdx = 0; outIdx < tx.numOutputs(); outIdx++) 
                        utxoPool.addUTXO(new UTXO(tx.getHash(), outIdx), tx.getOutput(outIdx));
                    // we need to remove the inputs from the UTXO Pool
                    for (int inIdx = 0; inIdx < tx.numInputs(); inIdx++) {
                        Transaction.Input input = tx.getInput(inIdx);
                        utxoPool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex));
                    }
                }
            }
        }
        return validTransactions.toArray(new Transaction[0]);
    }
}



/* “I acknowledge that I am aware of the academic integrity guidelines of this
course, and that I worked on this assignment independently without any
unauthorized help with coding or testing.” - Omar Salah Abdelkader */