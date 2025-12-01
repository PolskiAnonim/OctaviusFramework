package org.octavius.database.type.value

import kotlinx.serialization.Serializable
import org.octavius.data.annotation.DynamicallyMappable

/**
 * Zestaw klas testowych do weryfikacji, czy framework potrafi
 * obsługiwać polimorficzną listę `List<Any>`, w której znajdują się
 * zarówno standardowe `data class`, jak i `value class` opakowujące typy proste.
 */

// --- Wrappery 'value class' dla typów prostych ---

@Serializable
@DynamicallyMappable("user_id")
@JvmInline
value class UserId(val id: Int)

@Serializable
@DynamicallyMappable("session_token")
@JvmInline
value class SessionToken(val token: String)

@Serializable
@DynamicallyMappable("is_enabled_flag")
@JvmInline
value class IsEnabled(val status: Boolean)


@Serializable
@DynamicallyMappable("aaa")
@JvmInline
value class AAA(val status: List<String>)
// --- Standardowa 'data class' dla porównania ---

@Serializable
@DynamicallyMappable("user_action")
data class UserAction(val action: String, val timestamp: String)