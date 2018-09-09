/*
 * Copyright 2017 riddles.io (developers@riddles.io)
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 *     For the full copyright and license information, please view the LICENSE
 *     file that was distributed with this source code.
 */

package ufrn.alvarofpp.bot;

import java.util.ArrayList;
import java.util.Random;

import ufrn.alvarofpp.move.Move;
import ufrn.alvarofpp.move.MoveType;
import ufrn.alvarofpp.player.CharacterType;
import ufrn.alvarofpp.player.Player;

/**
 * bot.BotStarter
 *
 * Magic happens here. You should edit this file, or more specifically
 * the doMove() method to make your bot do more than random moves.
 *
 * @author Jim van Eeden - jim@riddles.io
 */
public class BotStarter {

    private Random random;

    private BotStarter() {
        this.random = new Random();
    }

    /**
     * Return a random character to play as
     * @return A random character
     */
    public CharacterType getCharacter() {
        CharacterType[] characters = CharacterType.values();
        return characters[this.random.nextInt(characters.length)];
    }

    /**
     * Does a move action. Edit this to make your bot smarter.
     * @param state The current state of the game
     * @return A Move object
     */
    public Move doMove(BotState state) {
        Player me = state.getPlayers().get(state.getMyName());
        MoveType bestMoveType = state.getField().getBestMoveTypes();

        if (bestMoveType == null ) {
            return new Move(); // No valid moves, pass
        }

        if (me.getBombs() <= 0) {
            return new Move(bestMoveType); // No bombs available
        }

        int bombTicks = this.random.nextInt(4) + 2; // random number 2 - 5

        return new Move(bestMoveType, bombTicks); // Drop bomb if available
    }

    public static void main(String[] args) {
        BotParser parser = new BotParser(new BotStarter());
        parser.run();
    }
}
