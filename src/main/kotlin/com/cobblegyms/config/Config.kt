import org.yaml.snakeyaml.Yaml
import java.io.InputStream

object Config {
    private val yaml = Yaml()
    private var configData: Map<String, Any>? = null

    fun loadConfig(inputStream: InputStream) {
        configData = yaml.load(inputStream)
    }

    fun getSeasonDuration(): Int? {
        return configData?.get("season_duration") as? Int
    }

    fun getRewardAmounts(): Map<String, Int>? {
        return configData?.get("reward_amounts") as? Map<String, Int>
    }

    fun getBanRules(): List<String>? {
        return configData?.get("ban_rules") as? List<String>
    }

    fun getGameModes(): List<String>? {
        return configData?.get("game_modes") as? List<String>
    }
}