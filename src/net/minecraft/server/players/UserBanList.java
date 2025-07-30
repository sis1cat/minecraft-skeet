package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.File;
import java.util.Objects;

public class UserBanList extends StoredUserList<GameProfile, UserBanListEntry> {
    public UserBanList(File p_11402_) {
        super(p_11402_);
    }

    @Override
    protected StoredUserEntry<GameProfile> createEntry(JsonObject pEntryData) {
        return new UserBanListEntry(pEntryData);
    }

    public boolean isBanned(GameProfile pProfile) {
        return this.contains(pProfile);
    }

    @Override
    public String[] getUserList() {
        return this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(GameProfile::getName).toArray(String[]::new);
    }

    protected String getKeyForUser(GameProfile pObj) {
        return pObj.getId().toString();
    }
}