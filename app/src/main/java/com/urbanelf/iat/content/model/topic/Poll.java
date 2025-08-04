/*
 * This file is part of Invision Archive Tools (IAT).
 *
 * Copyright (C) 2025 Mark Fisher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.urbanelf.iat.content.model.topic;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class Poll {
    private final String title;
    private final int participants;
    private final ArrayList<PollQuestion> questions;

    public Poll(JSONObject jsonObject) {
        title = jsonObject.getString("title");
        participants = jsonObject.getInt("participants");
        questions = new ArrayList<>();
        final JSONArray questionsArray = jsonObject.getJSONArray("questions");
        questionsArray.forEach(question -> questions.add(new PollQuestion((JSONObject) question)));
    }

    public String getTitle() {
        return title;
    }

    public int getParticipants() {
        return participants;
    }

    public ArrayList<PollQuestion> getQuestions() {
        return questions;
    }
}
