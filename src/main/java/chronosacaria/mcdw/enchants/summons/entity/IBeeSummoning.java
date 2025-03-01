package chronosacaria.mcdw.enchants.summons.entity;

public interface IBeeSummoning {
    void setLastSummonedBee(int time);
    int getLastSummonedBee();

    default boolean isReadyForBeeSummon(int summonerAge) {
        return summonerAge > getLastSummonedBee();
    }

    default void onBeeSummoned(int summonerAge) {
        setLastSummonedBee(summonerAge);
    }
}
