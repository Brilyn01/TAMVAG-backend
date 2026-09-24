package com.tamvagbackend.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tamvagbackend.domain.entity.*;
import com.tamvagbackend.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@Profile({"dev", "local", "default"})
@ConditionalOnProperty(name = "tamva.seeder.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeederService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeederService.class);

    private final PasswordEncoder passwordEncoder;
    private final InstitutionRepository institutionRepository;
    private final ApplicationRepository applicationRepository;
    private final CustomerRepository customerRepository;
    private final ConnectionRepository connectionRepository;
    private final AccountRepository accountRepository;
    private final ConsentRepository consentRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final LedgerService ledgerService;
    private final MultiCurrencyWalletService walletService;
    private final AdminUserRepository adminUserRepository;

    public DataSeederService(
            InstitutionRepository institutionRepository,
            ApplicationRepository applicationRepository,
            CustomerRepository customerRepository,
            ConnectionRepository connectionRepository,
            AccountRepository accountRepository,
            ConsentRepository consentRepository,
            ExchangeRateRepository exchangeRateRepository,
            LedgerService ledgerService,
            MultiCurrencyWalletService walletService,
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.institutionRepository = institutionRepository;
        this.applicationRepository = applicationRepository;
        this.customerRepository = customerRepository;
        this.connectionRepository = connectionRepository;
        this.accountRepository = accountRepository;
        this.consentRepository = consentRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.ledgerService = ledgerService;
        this.walletService = walletService;
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (institutionRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial data seeding.");
            return;
        }

        log.info("Seeding initial Ghanaian pilot ecosystem data into database...");

        // 1. Seed Institutions
        Institution mtn = institutionRepository.save(new Institution(UUID.fromString("11111111-1111-1111-1111-111111111111"), "MTN Mobile Money Ghana", "DEMI", "ACTIVE", "BOG/DEMI/MTN/2026"));
        Institution telecel = institutionRepository.save(new Institution(UUID.fromString("22222222-2222-2222-2222-222222222222"), "Telecel Cash Ghana", "DEMI", "ACTIVE", "BOG/DEMI/TC/2026"));
        Institution gcb = institutionRepository.save(new Institution(UUID.fromString("33333333-3333-3333-3333-333333333333"), "GCB Bank Ghana", "BANK", "ACTIVE", "BOG/BANK/GCB/001"));
        Institution ecobank = institutionRepository.save(new Institution(UUID.fromString("44444444-4444-4444-4444-444444444444"), "Ecobank Ghana", "BANK", "ACTIVE", "BOG/BANK/EBG/004"));
        Institution fidelity = institutionRepository.save(new Institution(UUID.fromString("55555555-5555-5555-5555-555555555555"), "Fidelity Bank Ghana", "BANK", "ACTIVE", "BOG/BANK/FBG/012"));

        // 2. Seed Partner Application

        String seedClientSecret = System.getenv("TAMVA_SEED_CLIENT_SECRET");

        if (seedClientSecret == null || seedClientSecret.isBlank()) {
            throw new IllegalStateException(
                    "TAMVA_SEED_CLIENT_SECRET must be configured"
            );
        }

        Application partnerApp = new Application();
        partnerApp.setInstitution(gcb);
        partnerApp.setClientId("app_gcb_pilot_2026");
        partnerApp.setClientSecretHash(
                passwordEncoder.encode(seedClientSecret)
        );
        partnerApp.setName("GCB Digital Risk & Credit Hub");
        partnerApp.setStatus("ACTIVE");
        partnerApp.setScopes("[\"risk:evaluate\", \"profile:read\", \"consent:create\", \"connector:sync\"]");
        applicationRepository.save(partnerApp);

        // 3. Seed Exchange Rates Matrix (GHS base against NGN, KES, ZAR, EGP, USD, GBP, EUR)
        seedExchangeRates();

        // 4. Seed Customers
        Customer kwame = customerRepository.save(new Customer(UUID.fromString("a1b2c3d4-0000-0000-0000-000000000001"), "CUS_KWAME_GH_01", "INDIVIDUAL", "ACTIVE"));
        Customer abena = customerRepository.save(new Customer(UUID.fromString("a1b2c3d4-0000-0000-0000-000000000002"), "CUS_ABENA_GH_02", "INDIVIDUAL", "ACTIVE"));

        kwame.setVerifiedAt(Instant.now().minus(180, ChronoUnit.DAYS));
        abena.setVerifiedAt(Instant.now().minus(90, ChronoUnit.DAYS));
        customerRepository.saveAll(List.of(kwame, abena));

        // 5. Connections & Consents
        Connection kwameMtnConnection = new Connection();
        kwameMtnConnection.setCustomer(kwame);
        kwameMtnConnection.setInstitution(mtn);
        kwameMtnConnection.setStatus("ACTIVE");
        kwameMtnConnection.setProviderRef("MOMO_233244123456");
        connectionRepository.save(kwameMtnConnection);

        Connection kwameGcbConnection = new Connection();
        kwameGcbConnection.setCustomer(kwame);
        kwameGcbConnection.setInstitution(gcb);
        kwameGcbConnection.setStatus("ACTIVE");
        kwameGcbConnection.setProviderRef("GCB_ACC_987654321");
        connectionRepository.save(kwameGcbConnection);

        Connection abenaTelecelConnection = new Connection();
        abenaTelecelConnection.setCustomer(abena);
        abenaTelecelConnection.setInstitution(telecel);
        abenaTelecelConnection.setStatus("ACTIVE");
        abenaTelecelConnection.setProviderRef("TC_233555987654");
        connectionRepository.save(abenaTelecelConnection);

        Consent kwameConsent = new Consent();
        kwameConsent.setCustomer(kwame);
        kwameConsent.setInstitution(gcb);
        kwameConsent.setPurpose("CREDIT_ASSESSMENT");
        kwameConsent.setScopes("[\"INCOME\", \"CASH_FLOW\", \"DEBT\"]");
        kwameConsent.setStatus("ACTIVE");
        kwameConsent.setGrantedAt(Instant.now().minus(30, ChronoUnit.DAYS));
        kwameConsent.setExpiresAt(Instant.now().plus(60, ChronoUnit.DAYS));
        consentRepository.save(kwameConsent);

        // 6. Seed Accounts
        Account kwameMomo = new Account();
        kwameMomo.setCustomer(kwame);
        kwameMomo.setInstitution(mtn);
        kwameMomo.setAccountType("MOBILE_MONEY");
        kwameMomo.setCurrency("GHS");
        kwameMomo.setMaskedIdentifier("024****456");
        kwameMomo.setAccountRefToken("TOK_KWAME_MOMO");
        accountRepository.save(kwameMomo);

        Account kwameBank = new Account();
        kwameBank.setCustomer(kwame);
        kwameBank.setInstitution(gcb);
        kwameBank.setAccountType("BANK_CURRENT");
        kwameBank.setCurrency("GHS");
        kwameBank.setMaskedIdentifier("201******321");
        kwameBank.setAccountRefToken("TOK_KWAME_GCB");
        accountRepository.save(kwameBank);

        Account abenaTelecel = new Account();
        abenaTelecel.setCustomer(abena);
        abenaTelecel.setInstitution(telecel);
        abenaTelecel.setAccountType("MOBILE_MONEY");
        abenaTelecel.setCurrency("GHS");
        abenaTelecel.setMaskedIdentifier("055****654");
        abenaTelecel.setAccountRefToken("TOK_ABENA_TC");
        accountRepository.save(abenaTelecel);

        // 7. Seed Multi-Currency Wallets
        Wallet kwameWallet = walletService.getOrCreateWallet(kwame);
        Wallet abenaWallet = walletService.getOrCreateWallet(abena);

        // Populate realistic balance amounts in wallets
        kwameWallet.getBalances().forEach(b -> {
            if ("GHS".equals(b.getCurrency())) b.setAvailableAmount(new BigDecimal("12500.0000"));
            if ("USD".equals(b.getCurrency())) b.setAvailableAmount(new BigDecimal("850.0000"));
            if ("NGN".equals(b.getCurrency())) b.setAvailableAmount(new BigDecimal("150000.0000"));
            if ("EUR".equals(b.getCurrency())) b.setAvailableAmount(new BigDecimal("300.0000"));
        });

        // 8. Seed Historical Transactions
        Instant now = Instant.now();
        ledgerService.recordTransaction(kwameMomo.getAccountId(), "SRC_MOMO_001", "IN", new BigDecimal("4500.00"), "GHS", now.minus(60, ChronoUnit.DAYS), "USSD", "MTN MoMo Salary Deposit", "Monthly Salary Deposit", "mtn_momo");
        ledgerService.recordTransaction(kwameMomo.getAccountId(), "SRC_MOMO_002", "OUT", new BigDecimal("1200.00"), "GHS", now.minus(45, ChronoUnit.DAYS), "MOBILE_APP", "Susu Savings Scheme", "Monthly Susu Savings", "mtn_momo");
        ledgerService.recordTransaction(kwameMomo.getAccountId(), "SRC_MOMO_003", "OUT", new BigDecimal("850.00"), "GHS", now.minus(30, ChronoUnit.DAYS), "USSD", "Utility & Rent Payment", "ECG Electricity Bill", "mtn_momo");
        ledgerService.recordTransaction(kwameBank.getAccountId(), "SRC_GCB_001", "IN", new BigDecimal("8000.00"), "GHS", now.minus(15, ChronoUnit.DAYS), "BRANCH", "Business Inflow", "Ghana Trade Business Inflow", "gcb_core");
        ledgerService.recordTransaction(kwameBank.getAccountId(), "SRC_GCB_002", "OUT", new BigDecimal("400.00"), "GHS", now.minus(5, ChronoUnit.DAYS), "MOBILE_APP", "Fidelity Loan Repay", "Loan Repayment", "gcb_core");

        ledgerService.recordTransaction(abenaTelecel.getAccountId(), "SRC_TC_001", "IN", new BigDecimal("3200.00"), "GHS", now.minus(20, ChronoUnit.DAYS), "USSD", "Freelance Design Payment", "UI Design Project Inflow", "telecel_cash");

        // 9. Seed Initial Bootstrap Super Admin (if not present)
        if (adminUserRepository.count() == 0) {
            AdminUser superAdmin = new AdminUser();
            superAdmin.setEmail("superadmin@tamva.com");
            superAdmin.setPasswordHash(passwordEncoder.encode("TamvaSuperAdmin!2026"));
            superAdmin.setFirstName("TAMVA");
            superAdmin.setLastName("SuperAdmin");
            superAdmin.setRole("SUPER_ADMIN");
            superAdmin.setOperationalRole("SECURITY_ADMINISTRATOR");
            superAdmin.setStatus("ACTIVE");
            adminUserRepository.save(superAdmin);
            log.info("Seeded initial bootstrap super admin: superadmin@tamva.com");
        }

        log.info("Successfully seeded TAMVA Ghanaian ecosystem with 5 institutions, sample customers, multi-currency wallets, transactions, and bootstrap super admin!");
    }

    private void seedExchangeRates() {
        createRate("GHS", "NGN", new BigDecimal("105.500000"), new BigDecimal("0.009478"));
        createRate("GHS", "KES", new BigDecimal("8.750000"), new BigDecimal("0.114285"));
        createRate("GHS", "ZAR", new BigDecimal("1.180000"), new BigDecimal("0.847457"));
        createRate("GHS", "EGP", new BigDecimal("3.250000"), new BigDecimal("0.307692"));
        createRate("GHS", "USD", new BigDecimal("0.065000"), new BigDecimal("15.384615"));
        createRate("GHS", "GBP", new BigDecimal("0.051000"), new BigDecimal("19.607843"));
        createRate("GHS", "EUR", new BigDecimal("0.060000"), new BigDecimal("16.666666"));
    }

    private void createRate(String base, String quote, BigDecimal rate, BigDecimal invRate) {
        ExchangeRate er = new ExchangeRate(base, quote, rate, invRate, new BigDecimal("0.0050"));
        exchangeRateRepository.save(er);
    }
}