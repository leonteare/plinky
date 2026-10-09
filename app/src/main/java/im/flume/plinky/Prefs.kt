package im.flume.plinky

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private fun p(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences("plinky", Context.MODE_PRIVATE)

    fun getInt(ctx: Context, key: String, def: Int): Int = p(ctx).getInt(key, def)

    fun setInt(ctx: Context, key: String, value: Int) {
        p(ctx).edit().putInt(key, value).apply()
    }

    fun addToInt(ctx: Context, key: String, delta: Int): Int {
        val v = getInt(ctx, key, 0) + delta
        setInt(ctx, key, v)
        return v
    }
}
