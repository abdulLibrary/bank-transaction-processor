package com.natwest.bank.service;

import com.natwest.bank.domain.Money;
import com.natwest.bank.domain.TransactionType;
import com.natwest.bank.exception.AccountNotFoundException;
import com.natwest.bank.exception.InsufficientFundsException;
import com.natwest.bank.exception.InvalidAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BankServiceTest {

    @Autowired
    private BankService bank;

    @Nested
    @DisplayName("Account lifecycle")
    class AccountLifecycle {

        @Test
        void opensAccountWithZeroBalance() {
            bank.openAccount("A1", Money.ZERO);
            assertEquals(Money.ZERO, bank.getBalance("A1"));
        }

        @Test
        void opensAccountWithInitialBalance() {
            bank.openAccount("A1", Money.of("250.00"));
            assertEquals(Money.of("250.00"), bank.getBalance("A1"));
        }

        @Test
        void rejectsDuplicateAccount() {
            bank.openAccount("A1", Money.ZERO);
            assertThrows(IllegalArgumentException.class, () -> bank.openAccount("A1", Money.ZERO));
        }

        @Test
        void rejectsNegativeInitialBalance() {
            assertThrows(InvalidAmountException.class,
                    () -> bank.openAccount("A1", Money.of("-1")));
        }
    }

    @Nested
    @DisplayName("Money movement")
    class MoneyMovement {

        @Test
        void depositIncreasesBalanceAndRecordsTransaction() {
            bank.openAccount("A1", Money.of("100.00"));
            bank.deposit("A1", Money.of("25.50"));
            assertEquals(Money.of("125.50"), bank.getBalance("A1"));
            assertEquals(1, bank.getTransactionHistory("A1").size());
            assertEquals(TransactionType.DEPOSIT, bank.getTransactionHistory("A1").get(0).getType());
        }

        @Test
        void withdrawalDecreasesBalance() {
            bank.openAccount("A1", Money.of("100.00"));
            bank.withdraw("A1", Money.of("40.00"));
            assertEquals(Money.of("60.00"), bank.getBalance("A1"));
        }

        @Test
        void rejectsOverdraft() {
            bank.openAccount("A1", Money.of("100.00"));
            assertThrows(InsufficientFundsException.class,
                    () -> bank.withdraw("A1", Money.of("150.00")));
            assertEquals(Money.of("100.00"), bank.getBalance("A1"));
        }

        @Test
        void rejectsZeroOrNegativeDeposit() {
            bank.openAccount("A1", Money.of("100.00"));
            assertThrows(InvalidAmountException.class, () -> bank.deposit("A1", Money.ZERO));
            assertThrows(InvalidAmountException.class, () -> bank.deposit("A1", Money.of("-5")));
        }

        @Test
        void transferMovesMoney() {
            bank.openAccount("A1", Money.of("100.00"));
            bank.openAccount("A2", Money.of("50.00"));
            bank.transfer("A1", "A2", Money.of("30.00"));
            assertEquals(Money.of("70.00"), bank.getBalance("A1"));
            assertEquals(Money.of("80.00"), bank.getBalance("A2"));
        }

        @Test
        void transferRecordsBothLegs() {
            bank.openAccount("A1", Money.of("100.00"));
            bank.openAccount("A2", Money.of("50.00"));
            bank.transfer("A1", "A2", Money.of("10.00"));
            var out = bank.getTransactionHistory("A1").get(0);
            var in = bank.getTransactionHistory("A2").get(0);
            assertEquals(TransactionType.TRANSFER_OUT, out.getType());
            assertEquals(TransactionType.TRANSFER_IN, in.getType());
            assertEquals("A2", out.getRelatedAccountId());
            assertEquals("A1", in.getRelatedAccountId());
        }

        @Test
        void rejectsTransferThatWouldOverdraw() {
            bank.openAccount("A1", Money.of("100.00"));
            bank.openAccount("A2", Money.of("50.00"));
            assertThrows(InsufficientFundsException.class,
                    () -> bank.transfer("A1", "A2", Money.of("200.00")));
            assertEquals(Money.of("100.00"), bank.getBalance("A1"));
            assertEquals(Money.of("50.00"), bank.getBalance("A2"));
        }

        @Test
        void rejectsSameAccountTransfer() {
            bank.openAccount("A1", Money.of("100.00"));
            assertThrows(IllegalArgumentException.class,
                    () -> bank.transfer("A1", "A1", Money.of("5")));
        }
    }

    @Nested
    @DisplayName("Error cases")
    class Errors {
        @Test
        void unknownAccountThrows() {
            assertThrows(AccountNotFoundException.class, () -> bank.getBalance("UNKNOWN"));
            assertThrows(AccountNotFoundException.class, () -> bank.getTransactionHistory("UNKNOWN"));
        }
    }
}
