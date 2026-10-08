package forge.player;

import com.google.common.collect.Lists;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.deck.Deck;
import forge.game.Match;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.util.Lang;
import forge.util.Localizer;

import java.util.List;
import java.util.Set;

/**
 * A four-player Two-Headed Giant game (2 teams of 2), all human-controlled, built the same
 * no-FModel/no-card-database way as {@link HumanControlledGame} - these tests exercise the shared
 * life pool, team-loss cascade, and team-pooled poison directly on {@link Player}, not through
 * actual spells, so no card database is needed.
 */
final class TwoHeadedGiantGame {

    final Game game;
    final Player teamAPrimary;
    final Player teamAPartner;
    final Player teamBPrimary;
    final Player teamBPartner;

    TwoHeadedGiantGame() {
        Localizer.getInstance().initialize("en-US", "res/languages/");
        Lang.createInstance("en-US");

        final List<RegisteredPlayer> players = Lists.newArrayList(
                new RegisteredPlayer(new Deck()).setPlayer(lobbyPlayer("Team A - 1")),
                new RegisteredPlayer(new Deck()).setPlayer(lobbyPlayer("Team A - 2")),
                new RegisteredPlayer(new Deck()).setPlayer(lobbyPlayer("Team B - 1")),
                new RegisteredPlayer(new Deck()).setPlayer(lobbyPlayer("Team B - 2")));
        players.get(0).setTeamNumber(0);
        players.get(1).setTeamNumber(0);
        players.get(2).setTeamNumber(1);
        players.get(3).setTeamNumber(1);

        final GameRules rules = new GameRules(GameType.TwoHeadedGiant);
        rules.setAppliedVariants(Set.of(GameType.TwoHeadedGiant));
        rules.setPoisonCountersToLose(15);

        game = new Game(players, rules, new Match(rules, players, "Test"));
        teamAPrimary = game.getPlayers().get(0);
        teamAPartner = game.getPlayers().get(1);
        teamBPrimary = game.getPlayers().get(2);
        teamBPartner = game.getPlayers().get(3);

        // Mirrors what RegisteredPlayer.forVariants would compute (20 * 1.5 = 30) - set directly on
        // every player rather than relying on mirroring for this initial setup, since mirroring is
        // for in-game life changes, not establishing the starting total.
        for (final Player p : game.getPlayers()) {
            p.setStartingLife(30);
        }
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
