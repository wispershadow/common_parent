package io.wispershadow.tech.common.el

import java.util.*

class TransactionMonitorDTO {
    var paymentId: UUID? = null
    var createdAt: Date? = null
    var clientId: UUID? = null

    var payment: TransactionDTO.PaymentDTO? = null
}

class TransactionDTO {
    class PaymentDTO {
        lateinit var beneficiary: Beneficiary
    }
}

class Beneficiary {
    lateinit var bankDetails: BankAccount
}

class BankAccount {
    lateinit var bankCountryCode: String
}