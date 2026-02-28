package dev.mastodon.streaming.db.user

import dev.mastodon.streaming.db.account.Account
import io.quarkus.hibernate.orm.panache.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.ColumnDefault
import java.net.InetAddress
import java.time.LocalDateTime

@Entity
@Table(
    name = "users", indexes = [
        Index(
            name = "index_users_on_email",
            columnList = "email",
            unique = true
        ),
        Index(
            name = "index_users_on_reset_password_token",
            columnList = "reset_password_token",
            unique = true
        ),
        Index(
            name = "index_users_on_confirmation_token",
            columnList = "confirmation_token",
            unique = true
        ),
        Index(
            name = "index_users_on_unconfirmed_email",
            columnList = "unconfirmed_email"
        ),
        Index(
            name = "index_users_on_account_id",
            columnList = "account_id"
        ),
        Index(
            name = "index_users_on_created_by_application_id",
            columnList = "created_by_application_id"
        ),
        Index(
            name = "index_users_on_role_id",
            columnList = "role_id"
        )]
)
class User : PanacheEntityBase() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0

    @NotNull
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null

    @NotNull
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null

    @NotNull
    @ColumnDefault("''")
    @Column(name = "encrypted_password", length = Integer.MAX_VALUE)
    var encryptedPassword: String? = null

    @Column(name = "reset_password_token", length = Integer.MAX_VALUE)
    var resetPasswordToken: String? = null

    @Column(name = "reset_password_sent_at")
    var resetPasswordSentAt: LocalDateTime? = null

    @NotNull
    @ColumnDefault("0")
    @Column(name = "sign_in_count")
    var signInCount: Int? = null

    @Column(name = "current_sign_in_at")
    var currentSignInAt: LocalDateTime? = null

    @Column(name = "last_sign_in_at")
    var lastSignInAt: LocalDateTime? = null

    @Column(name = "confirmation_token", length = Integer.MAX_VALUE)
    var confirmationToken: String? = null

    @Column(name = "confirmed_at")
    var confirmedAt: LocalDateTime? = null

    @Column(name = "confirmation_sent_at")
    var confirmationSentAt: LocalDateTime? = null

    @Column(name = "unconfirmed_email", length = Integer.MAX_VALUE)
    var unconfirmedEmail: String? = null

    @Column(name = "locale", length = Integer.MAX_VALUE)
    var locale: String? = null

    @Column(name = "consumed_timestep")
    var consumedTimestep: Int? = null

    @NotNull
    @ColumnDefault("false")
    @Column(name = "otp_required_for_login")
    var otpRequiredForLogin: Boolean? = null

    @Column(name = "last_emailed_at")
    var lastEmailedAt: LocalDateTime? = null

    @Column(name = "otp_backup_codes")
    var otpBackupCodes: List<String>? = null

    @NotNull
    @ColumnDefault("false")
    @Column(name = "disabled")
    var disabled: Boolean? = null

    @Column(name = "invite_id")
    var inviteId: Long? = null

    @Column(name = "chosen_languages")
    var chosenLanguages: List<String>? = null

    @Column(name = "created_by_application_id")
    var createdByApplicationId: Long? = null

    @NotNull
    @ColumnDefault("true")
    @Column(name = "approved", nullable = false)
    var approved: Boolean? = null

    @Column(name = "sign_in_token", length = Integer.MAX_VALUE)
    var signInToken: String? = null

    @Column(name = "sign_in_token_sent_at")
    var signInTokenSentAt: LocalDateTime? = null

    @Column(name = "webauthn_id", length = Integer.MAX_VALUE)
    var webauthnId: String? = null

    @Column(name = "sign_up_ip")
    var signUpIp: InetAddress? = null

    @Column(name = "skip_sign_in_token")
    var skipSignInToken: Boolean? = null

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UserRole::class)
    @JoinColumn(name = "role_id")
    var role: UserRole? = null

    @Column(name = "settings", length = Integer.MAX_VALUE)
    var settings: String? = null

    @Column(name = "time_zone", length = Integer.MAX_VALUE)
    var timeZone: String? = null

    @Column(name = "otp_secret", length = Integer.MAX_VALUE)
    var otpSecret: String? = null

    @Column(name = "age_verified_at")
    var ageVerifiedAt: LocalDateTime? = null

    @NotNull
    @ColumnDefault("false")
    @Column(name = "require_tos_interstitial")
    var requireTosInterstitial: Boolean? = null

    @NotNull
    @ColumnDefault("''")
    @Column(name = "email", length = Integer.MAX_VALUE)
    lateinit var email: String

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Account::class)
    @JoinColumn(name = "account_id")
    lateinit var account: Account
}
