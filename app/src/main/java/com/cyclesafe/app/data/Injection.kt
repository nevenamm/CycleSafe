
import android.content.Context
import com.cyclesafe.app.data.preferences.UserPreferencesRepository
import com.cyclesafe.app.data.repository.PoiRepository
import com.cyclesafe.app.data.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore

object Injection {

    fun providePoiRepository(context: Context): PoiRepository {
        return PoiRepository(FirebaseFirestore.getInstance())
    }

    fun provideUserRepository(context: Context): UserRepository {
        return UserRepository(FirebaseFirestore.getInstance())
    }

    fun provideUserPreferencesRepository(context: Context): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }
}