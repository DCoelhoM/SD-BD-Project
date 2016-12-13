/*==============================================================*/
/* Table: auction_history*/
/*==============================================================*/
drop table if exists auction_history;
create table auction_history
(
  auction_id           mediumint not null AUTO_INCREMENT,
  title                char(32),
  description          char(128),
  deadline             datetime,
  edited               datetime,
  primary key (auction_id, edited)
);

/*==============================================================*/
/* Table: auction*/
/*==============================================================*/
drop table if exists auction;
create table auction
(
  id                   mediumint not null auto_increment,
  username             char(16) not null,
  state                char(16) not null,
  code                 char(16) not null,
  title                char(32) not null,
  description          char(128) not null,
  created_date         datetime not null,
  deadline             datetime not null,
  amount               decimal(10,2) not null,
  primary key (id)
);

/*==============================================================*/
/* Table: bid*/
/*==============================================================*/
drop table if exists bid;
create table bid
(
  id                   mediumint not null auto_increment,
  auction_id           mediumint not null,
  username             char(16) not null,
  bid_date             datetime not null,
  amount               decimal(10,2) not null,
  primary key (id)
);

/*==============================================================*/
/* Table: message*/
/*==============================================================*/
drop table if exists message;
create table message
(
  id                   mediumint not null auto_increment,
  auction_id           mediumint not null,
  username             char(16) not null,
  message_date         datetime not null,
  text                 char(128) not null,
  primary key (id)
);

/*==============================================================*/
/* Table: bid_notification*/
/*==============================================================*/
drop table if exists bid_notification;
create table bid_notification
(
  id                   mediumint not null auto_increment,
  bid_id               mediumint not null,
  username             char(16),
  primary key (id)
);

/*==============================================================*/
/* Table: message_notification*/
/*==============================================================*/
drop table if exists message_notification;
create table message_notification
(
  id                   mediumint not null auto_increment,
  message_id           mediumint not null,
  username             char(16),
  primary key (id)
);

/*==============================================================*/
/* Table: user                                                  */
/*==============================================================*/
drop table if exists user;
create table user
(
  username             char(16) not null,
  password             char(32) not null,
  state                char(8) not null,
  primary key (username)
);

alter table auction_history add constraint FK_relationship_1 foreign key (auction_id)
references auction (id) on delete restrict;

alter table auction add constraint FK_relationship_2 foreign key (username)
references user (username) on delete restrict;

alter table bid add constraint FK_relationship_3 foreign key (username)
references user (username) on delete restrict;

alter table bid add constraint FK_relationship_4 foreign key (auction_id)
references auction (id) on delete restrict;

alter table message add constraint FK_relationship_5 foreign key (auction_id)
references auction (id) on delete restrict;

alter table message add constraint FK_relationship_6 foreign key (username)
references user (username) on delete restrict;

alter table bid_notification add constraint FK_relationship_7 foreign key (bid_id)
references bid (id) on delete cascade;

alter table bid_notification add constraint FK_relationship_8 foreign key (username)
references user (username) on delete cascade;

alter table message_notification add constraint FK_relationship_9 foreign key (message_id)
references message (id) on delete cascade;

alter table message_notification add constraint FK_relationship_10 foreign key (username)
references user (username) on delete cascade;
