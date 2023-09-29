
create table "players" (
	"id" uuid not null primary key,
	"name" character varying not null,
	"created_at" timestamp not null,
	"updated_at" timestamp not null,
	"avatar" jsonb null
);

create table "games" (
	"id" uuid not null primary key,
	"name" character varying not null,
	"created_at" timestamp not null,
	"updated_at" timestamp not null,
	"password_hash" character varying null,
	"playable_cards" int[]
);

create table "rounds" (
	"id" uuid not null primary key,
	"game_id" uuid not null,
	"topic" character varying not null,
	"created_at" timestamp not null,
	"updated_at" timestamp not null,
	"ended_at" timestamp null,
	"ended_by" uuid not null,
	constraint "fk_game" foreign key("game_id") references "games"("id") on delete cascade,
	constraint "fk_ended_by" foreign key("ended_by") references "players"("id") on delete set null
);

create index "idx_rounds_game_id" on "rounds"("game_id");

create table "votes" (
	"round_id" uuid not null,
	"player_id" uuid not null,
	"created_at" timestamp not null,
	"card" int not null,
	primary key("round_id", "player_id"),
	constraint "fk_round" foreign key("round_id") references "rounds"("id") on delete cascade,
	constraint "fk_player" foreign key("player_id") references "players"("id") on delete cascade
);

create table "player_games" (
	"player_id" uuid not null,
	"game_id" uuid not null,
	"joined_at" timestamp not null,
	primary key ("player_id", "game_id"),
	constraint "fk_player" foreign key("player_id") references "players"("id") on delete cascade,
	constraint "fk_game" foreign key("game_id") references "games"("id") on delete cascade
);