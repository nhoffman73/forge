package forge.player;

import com.google.common.collect.Lists;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Engine-level tests for the Two-Headed Giant team mechanics added to {@code Player}/{@code
 * GameAction}: the shared life pool, team-loss cascade, and team-pooled poison. These drive
 * {@link Player} directly (no card database) rather than through the heavier
 * {@code gamesimulationtests} DSL, which only supports exactly two players.
 */
public class TwoHeadedGiantGameShould {

    @Test
    public void mirrorLifeChangesOntoTheTeammateButNotTheOtherTeam() {
        TwoHeadedGiantGame g = new TwoHeadedGiantGame();

        g.teamAPrimary.loseLife(10, false, false, null);

        assertThat(g.teamAPrimary.getLife()).isEqualTo(20);
        assertThat(g.teamAPartner.getLife()).isEqualTo(20);
        assertThat(g.teamBPrimary.getLife()).isEqualTo(30);
        assertThat(g.teamBPartner.getLife()).isEqualTo(30);

        g.teamAPartner.gainLife(5, null, null);

        assertThat(g.teamAPrimary.getLife()).isEqualTo(25);
        assertThat(g.teamAPartner.getLife()).isEqualTo(25);
    }

    @Test
    public void makeBothTeammatesLoseWhenTheSharedLifeTotalReachesZero() {
        TwoHeadedGiantGame g = new TwoHeadedGiantGame();

        g.teamAPrimary.loseLife(30, false, false, null);
        g.game.getAction().checkGameOverCondition();

        assertThat(g.teamAPrimary.hasLost()).isTrue();
        assertThat(g.teamAPartner.hasLost()).isTrue();
        assertThat(g.teamBPrimary.hasLost()).isFalse();
        assertThat(g.teamBPartner.hasLost()).isFalse();
    }

    @Test
    public void makeBothTeammatesLoseOnCombinedPoisonAcrossTheTeamButNotBelowThreshold() {
        TwoHeadedGiantGame g = new TwoHeadedGiantGame();
        g.teamAPrimary.setPoisonCounters(7, g.teamBPrimary);
        g.teamAPartner.setPoisonCounters(7, g.teamBPrimary);

        g.game.getAction().checkGameOverCondition();
        assertThat(g.teamAPrimary.hasLost()).isFalse();
        assertThat(g.teamAPartner.hasLost()).isFalse();

        g.teamAPartner.setPoisonCounters(8, g.teamBPrimary); // combined 7 + 8 = 15
        g.game.getAction().checkGameOverCondition();

        assertThat(g.teamAPrimary.hasLost()).isTrue();
        assertThat(g.teamAPartner.hasLost()).isTrue();
        assertThat(g.teamBPrimary.hasLost()).isFalse();
        assertThat(g.teamBPartner.hasLost()).isFalse();
    }

    @Test
    public void cascadeANonLifeLossToTheTeammate() {
        // Decking out, a targeted "loses the game" effect, or a concession only naturally fails the
        // one affected player - the shared life pool doesn't help here, since nothing reduced it.
        TwoHeadedGiantGame g = new TwoHeadedGiantGame();

        g.teamAPrimary.concede();
        g.game.getAction().checkGameOverCondition();

        assertThat(g.teamAPrimary.hasLost()).isTrue();
        assertThat(g.teamAPartner.hasLost()).isTrue();
        assertThat(g.teamBPrimary.hasLost()).isFalse();
        assertThat(g.teamBPartner.hasLost()).isFalse();
    }

    @Test
    public void notCascadeALossToAWinningPlayersOwnTeammate() {
        TwoHeadedGiantGame g = new TwoHeadedGiantGame();

        g.teamAPrimary.altWinBySpellEffect("Test Card");
        g.game.getAction().checkGameOverCondition();

        assertThat(g.teamAPrimary.hasWon()).isTrue();
        assertThat(g.teamAPartner.hasLost()).isFalse();
        assertThat(g.teamBPrimary.hasLost()).isTrue();
        assertThat(g.teamBPartner.hasLost()).isTrue();
    }

    @Test
    public void startEachTeamAtOneAndAHalfTimesTheFormatsStartingLife() {
        // 810.4: 30 on top of Constructed, 60 on top of Commander
        assertThat(RegisteredPlayer.forVariants(4, Set.of(GameType.TwoHeadedGiant),
                new Deck(), null, false, null, null).getStartingLife()).isEqualTo(30);
        assertThat(RegisteredPlayer.forVariants(4, Set.of(GameType.TwoHeadedGiant, GameType.Commander),
                new Deck(), null, false, null, null).getStartingLife()).isEqualTo(60);
    }

    @Test
    public void makeBothTeammatesLoseOnCombinedDamageFromOneCommander() {
        TwoHeadedGiantGame g = new TwoHeadedGiantGame(true);
        final Card commander = new Card(g.game.nextCardId(), g.game);
        g.teamAPrimary.addCommanderDamage(commander, 10);
        g.teamAPartner.addCommanderDamage(commander, 10);

        g.game.getAction().checkGameOverCondition();
        assertThat(g.teamAPrimary.hasLost()).isFalse();
        assertThat(g.teamAPartner.hasLost()).isFalse();

        g.teamAPartner.addCommanderDamage(commander, 1); // combined 10 + 11 = 21
        g.game.getAction().checkGameOverCondition();

        assertThat(g.teamAPrimary.hasLost()).isTrue();
        assertThat(g.teamAPartner.hasLost()).isTrue();
        assertThat(g.teamBPrimary.hasLost()).isFalse();
        assertThat(g.teamBPartner.hasLost()).isFalse();
    }

    @Test
    public void notCascadeALossToASharedTeamNumberOutsideTwoHeadedGiant() {
        // Archenemy and generic multiplayer also group players under LobbySlot/RegisteredPlayer team
        // numbers, but only Two-Headed Giant shares a loss condition across that team - regression
        // guard for a bug where the cascade ran for any shared team number, not just 2HG.
        final List<RegisteredPlayer> players = Lists.newArrayList(
                new RegisteredPlayer(new Deck()).setPlayer(lobbyPlayer("Hero 1")),
                new RegisteredPlayer(new Deck()).setPlayer(lobbyPlayer("Hero 2")),
                new RegisteredPlayer(new Deck()).setPlayer(lobbyPlayer("Hero 3")),
                new RegisteredPlayer(new Deck()).setPlayer(lobbyPlayer("Hero 4")));
        players.get(0).setTeamNumber(0);
        players.get(1).setTeamNumber(0);
        players.get(2).setTeamNumber(1);
        players.get(3).setTeamNumber(1);

        final GameRules rules = new GameRules(GameType.Constructed); // no TwoHeadedGiant variant applied
        final Game game = new Game(players, rules, new Match(rules, players, "Test"));
        final Player hero1 = game.getPlayers().get(0);
        final Player hero2 = game.getPlayers().get(1);

        hero1.concede();
        game.getAction().checkGameOverCondition();

        assertThat(hero1.hasLost()).isTrue();
        assertThat(hero2.hasLost()).isFalse();
    }

    private static LobbyPlayerHuman lobbyPlayer(final String name) {
        return new LobbyPlayerHuman(name) {
            @Override
            public Player createIngamePlayer(final Game game, final int id) {
                final Player player = new Player(getName(), game, id);
                player.setFirstController(new PlayerControllerHuman(game, player, this));
                return player;
            }
        };
    }
}
