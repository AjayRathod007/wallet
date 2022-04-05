package com.walletApplication.Services;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.walletApplication.Dao.MerchantWalletDao;
import com.walletApplication.Dao.TransactionDao;
import com.walletApplication.Dao.UserWalletDao;
import com.walletApplication.Dto.TransactionCreationBody;
import com.walletApplication.Dto.TransactionUpdateEntity;
import com.walletApplication.Entities.MerchantWalletEntity;
import com.walletApplication.Entities.TransactionEntity;
import com.walletApplication.Entities.UserWalletEntity;
import com.walletApplication.Entities.Wallet;
import com.walletApplication.Enum.MerchantStatus;
import com.walletApplication.Enum.TransactionStatus;
import com.walletApplication.Enum.TransactionType;

@Service
public class TransactionService {
	
	Logger logger = LoggerFactory.getLogger(TransactionService.class);
	
	@Autowired
	private TransactionDao transactionDao;

	@Autowired
	private UserWalletDao userWalletDao;

	@Autowired
	private MerchantWalletDao merchantWalletDao;

	@Autowired
	private UserWalletService userWalletService;

	@Autowired
	private MerchantWalletService merchantWalletService;

	public List<TransactionEntity> getAllTransaction() {

		return transactionDao.findAll();
	}

	public TransactionEntity getById(int transactionId) {
		return transactionDao.findById(transactionId);

	}

	public TransactionEntity sendMoneyToUser(TransactionCreationBody transaction) {
		UserWalletEntity senderWallet;
		UserWalletEntity receiverWallet;
		senderWallet = userWalletService.getById(transaction.getSenderUserId());
		receiverWallet = userWalletService.getById(transaction.getReceiverUserId());
		
		logger.info("sender wallet " + senderWallet.toString());
		logger.info("receiver wallet " + receiverWallet.toString());
		
		TransactionEntity newTransaction = createTransaction(transaction);
		newTransaction.setTransactionType(TransactionType.USER.toString());
		newTransaction = updateWallet(senderWallet, receiverWallet, newTransaction, transaction);
		userWalletDao.save(senderWallet);
		userWalletDao.save(receiverWallet);
		transactionDao.save(newTransaction);
		return newTransaction;
	}

	@Transactional()
	public TransactionEntity sendMoneyToMerchant(TransactionCreationBody transaction) throws Exception {

		UserWalletEntity senderWallet;
		MerchantWalletEntity receiverWallet;
		senderWallet = userWalletService.getById(transaction.getSenderUserId());
		logger.info("user sender wallet {}",senderWallet.toString());
		receiverWallet = merchantWalletService.getById(transaction.getReceiverUserId());
		logger.info("mer receiver wallet {}",receiverWallet.toString());
		TransactionEntity newTransaction = createTransaction(transaction);
		logger.info("tra created");
		newTransaction.setTransactionType(TransactionType.MERCHANT.toString());
		newTransaction = updateWallet(senderWallet, receiverWallet, newTransaction, transaction);
		userWalletDao.save(senderWallet);
		//if(receiverWallet.getMerchantId().equals("8519042337"))
		  //    throw new Exception("manual exception");
		transactionDao.save(newTransaction);
		merchantWalletDao.save(receiverWallet);
		receiverWallet.setStatus(MerchantStatus.INACTIVE.name());
		return newTransaction;

	}

	public TransactionEntity updateWallet(Wallet senderWallet, Wallet receiverWallet, TransactionEntity newTransaction,
			TransactionCreationBody transaction) {

		if (senderWallet.getStatus().equalsIgnoreCase("INACTIVE")
				|| receiverWallet.getStatus().equalsIgnoreCase("INACTIVE")
				|| senderWallet.getAmount() < transaction.getAmount()) {

			newTransaction.setStatus(TransactionStatus.FAILED.toString());

		} else {
			
			 senderWallet.setAmount(senderWallet.getAmount() - transaction.getAmount());
			receiverWallet.setAmount(receiverWallet.getAmount() + transaction.getAmount());
			newTransaction.setStatus(TransactionStatus.RECEIVED.toString());
		}

		return newTransaction;
	}

	// TO CREATE TRANSACNITION OBJECT
	public TransactionEntity createTransaction(TransactionCreationBody transaction) {
		TransactionEntity newTransaction = new TransactionEntity();
		newTransaction.setDate(new Date());
		newTransaction.setSenderUserId(transaction.getSenderUserId());
		newTransaction.setReceiverUserId(transaction.getReceiverUserId());
		newTransaction.setAmount(transaction.getAmount());
		return newTransaction;
	}

	public TransactionEntity updateTransaction(int transactionId, TransactionUpdateEntity transaction) {
		TransactionEntity newTransaction = transactionDao.findById(transactionId);
		newTransaction.setAmount(transaction.getAmount());
		newTransaction.setStatus(transaction.getStatus());
		newTransaction.setTransactionType(transaction.getTransactionType());
		return transactionDao.save(newTransaction);

	}

	public void deleteTransaction(int transactionId) {

		TransactionEntity entity = transactionDao.findById(transactionId);
		transactionDao.delete(entity);
	}
	
	public List<TransactionEntity> getAllTransactionByDate(Date startDate,Date endDate){
		return transactionDao.findByDateBetweenAnd(startDate,endDate);
	}

}
