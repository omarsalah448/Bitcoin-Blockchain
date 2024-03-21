// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class BlockChain { 
    public static final int CUT_OFF_AGE = 10;
    Block genesisBlock;
    Map<byte[], BlockState> blockStatesMap = new HashMap<byte[], BlockState>();
    TransactionPool transactionPool = new TransactionPool();

    private class BlockState {
        Block block;
        long date;
        int height;
        UTXOPool utxoPool;
        private BlockState(Block block, int height, UTXOPool utxoPool) {
            this.block = block;
            this.date = System.currentTimeMillis();
            this.height = height;
            this.utxoPool = utxoPool;
        }
    }
    /**
     * create an empty blockchain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        this.genesisBlock = genesisBlock;
        UTXOPool utxoPool = new UTXOPool();
        Transaction tx = genesisBlock.getCoinbase();
        utxoPool.addUTXO(new UTXO(tx.getHash(), 0), tx.getOutput(0));
        this.blockStatesMap.put(genesisBlock.getHash(), new BlockState(genesisBlock, 1, utxoPool));
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        BlockState maxBlock = blockStatesMap.values().toArray(new BlockState[0])[0];
        for (BlockState blockState : blockStatesMap.values()) {
            if ((blockState.height > maxBlock.height) || 
                (blockState.height == maxBlock.height && blockState.date < maxBlock.date))
                maxBlock = blockState;
        }
        return maxBlock.block;
    }

    /** Get the blockchain height */
    public int getBlockchainHeight() {
        Block block = getMaxHeightBlock();
        return blockStatesMap.get(block.getHash()).height;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        Block maxHeightBlock = getMaxHeightBlock();
        return blockStatesMap.get(maxHeightBlock.getHash()).utxoPool;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return this.transactionPool;
    }

    /**
     * Add {@code block} to the blockchain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}, where maxHeight is 
     * the current height of the blockchain.
	 * <p>
	 * Assume the Genesis block is at height 1.
     * For example, you can try creating a new block over the genesis block (i.e. create a block at 
	 * height 2) if the current blockchain height is less than or equal to CUT_OFF_AGE + 1. As soon as
	 * the current blockchain height exceeds CUT_OFF_AGE + 1, you cannot create a new block at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS
        // if block claims to be the genesis block
        if (block.getPrevBlockHash() == null)
            return false;
        int maxHeight = getBlockchainHeight();
        // get the corresponding block state to the parent block
        BlockState parentState = blockStatesMap.get(block.getPrevBlockHash());
        if (parentState == null)
            return false;
        // if the block's height is invalid (cut off age condition)
        if (parentState.height + 1 <= (maxHeight - CUT_OFF_AGE)) 
            return false;
        TxHandler txHandler = new TxHandler(parentState.utxoPool);
        // get all the block's transactions
        Transaction[] blockTransactions = block.getTransactions().toArray(new Transaction[0]);
        // get all the valid transactions within this block
        Transaction[] validTransactions = txHandler.handleTxs(blockTransactions);
        // if any transaction is invalid then return false
        if (!compareTransactions(blockTransactions, validTransactions))
            return false;
        // add the coinbase to the UTXO Pool
        Transaction coinBaseTx = block.getCoinbase();
        UTXOPool utxoPool = txHandler.getUTXOPool();
        utxoPool.addUTXO(new UTXO(coinBaseTx.getHash(), 0), coinBaseTx.getOutput(0));
        blockStatesMap.put(block.getHash(), new BlockState(block, parentState.height + 1, utxoPool));
        // remove the block's transactions from the pool
        for (Transaction tx : block.getTransactions()) 
            transactionPool.removeTransaction(tx.getHash());
        // remove all the old blocks
        blockStatesMap.values().removeIf(value -> value.height <= (maxHeight - CUT_OFF_AGE));
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        this.transactionPool.addTransaction(tx);
    }

    public static boolean compareTransactions(Transaction[] txs1, Transaction[] txs2) {
        HashSet<Transaction> set1 = new HashSet<Transaction>(Arrays.asList(txs1));
        HashSet<Transaction> set2 = new HashSet<Transaction>(Arrays.asList(txs2));
        return set1.equals(set2);
    }
}

/* “I acknowledge that I am aware of the academic integrity guidelines of this
course, and that I worked on this assignment independently without any
unauthorized help with coding or testing.” - Omar Salah Abdelkader */