package app.smsexpense.data

import androidx.room.TypeConverter
import app.smsexpense.core.Direction
import java.math.BigDecimal

class Converters {
    @TypeConverter fun fromBigDecimal(v: BigDecimal): String = v.toPlainString()
    @TypeConverter fun toBigDecimal(v: String): BigDecimal = BigDecimal(v)
    @TypeConverter fun fromDirection(v: Direction): String = v.name
    @TypeConverter fun toDirection(v: String): Direction = Direction.valueOf(v)
}
