from __future__ import annotations

import html
import re
import subprocess
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SQL_PATH = ROOT / "deploy" / "bootstrap-db.sql"
OUT_DIR = ROOT / "docs" / "architecture" / "er-diagrams"


@dataclass(frozen=True)
class Column:
    name: str
    type_name: str
    flags: tuple[str, ...]


@dataclass(frozen=True)
class Table:
    database: str
    name: str
    columns: tuple[Column, ...]


@dataclass(frozen=True)
class Relation:
    src_table: str
    src_col: str
    dst_table: str
    dst_col: str
    label: str
    external: bool = False


SERVICES = {
    "user-service": {
        "database": "user_service_db",
        "title": "user-service",
        "relations": (
            Relation(
                "user_feedback_projection",
                "user_id",
                "sys_user",
                "id",
                "logical user reference; no DB FK",
            ),
        ),
        "external_tables": {},
    },
    "activity-service": {
        "database": "activity_service_db",
        "title": "activity-service",
        "relations": (
            Relation(
                "vol_registration",
                "activity_id",
                "vol_activity",
                "id",
                "registration belongs to activity; no DB FK",
            ),
            Relation(
                "vol_registration",
                "user_id",
                "external_sys_user",
                "id",
                "cross-service user id; no DB FK",
                True,
            ),
            Relation(
                "vol_activity",
                "creator_id",
                "external_sys_user",
                "id",
                "cross-service creator id; no DB FK",
                True,
            ),
        ),
        "external_tables": {
            "external_sys_user": Table(
                "user_service_db",
                "external: sys_user",
                (
                    Column("id", "BIGINT", ("PK",)),
                    Column("owned_by", "user-service", ()),
                ),
            ),
        },
    },
    "announcement-service": {
        "database": "announcement_service_db",
        "title": "announcement-service",
        "relations": (
            Relation(
                "vol_announcement_activity",
                "announcement_id",
                "vol_announcement",
                "id",
                "announcement activity mapping; no DB FK",
            ),
            Relation(
                "vol_announcement_attachment",
                "announcement_id",
                "vol_announcement",
                "id",
                "attachment belongs to announcement; no DB FK",
            ),
            Relation(
                "vol_announcement",
                "activity_id",
                "vol_announcement_activity_projection",
                "id",
                "local activity projection; no DB FK",
            ),
            Relation(
                "vol_announcement_activity",
                "activity_id",
                "vol_announcement_activity_projection",
                "id",
                "local activity projection; no DB FK",
            ),
            Relation(
                "vol_announcement",
                "publisher_id",
                "external_sys_user",
                "id",
                "cross-service publisher id; no DB FK",
                True,
            ),
        ),
        "external_tables": {
            "external_sys_user": Table(
                "user_service_db",
                "external: sys_user",
                (
                    Column("id", "BIGINT", ("PK",)),
                    Column("owned_by", "user-service", ()),
                ),
            ),
        },
    },
    "feedback-service": {
        "database": "feedback_service_db",
        "title": "feedback-service",
        "relations": (
            Relation(
                "feedback_message",
                "feedback_id",
                "feedback",
                "id",
                "message belongs to feedback; no DB FK",
            ),
            Relation(
                "feedback_message_attachment",
                "feedback_id",
                "feedback",
                "id",
                "attachment belongs to feedback; no DB FK",
            ),
            Relation(
                "feedback_message_attachment",
                "message_id",
                "feedback_message",
                "id",
                "attachment belongs to message; no DB FK",
            ),
            Relation(
                "feedback",
                "user_id",
                "external_sys_user",
                "id",
                "cross-service user id; no DB FK",
                True,
            ),
            Relation(
                "feedback_message",
                "sender_id",
                "external_sys_user",
                "id",
                "cross-service sender id; no DB FK",
                True,
            ),
            Relation(
                "feedback",
                "closed_by",
                "external_sys_user",
                "id",
                "cross-service operator id; no DB FK",
                True,
            ),
        ),
        "external_tables": {
            "external_sys_user": Table(
                "user_service_db",
                "external: sys_user",
                (
                    Column("id", "BIGINT", ("PK",)),
                    Column("owned_by", "user-service", ()),
                ),
            ),
        },
    },
}


def parse_bootstrap_sql(sql_path: Path) -> dict[str, dict[str, Table]]:
    databases: dict[str, dict[str, Table]] = {}
    current_db: str | None = None
    current_table: str | None = None
    table_lines: list[str] = []

    use_re = re.compile(r"^\s*USE\s+([A-Za-z0-9_]+)\s*;", re.IGNORECASE)
    table_re = re.compile(
        r"^\s*CREATE\s+TABLE\s+IF\s+NOT\s+EXISTS\s+([A-Za-z0-9_]+)\s*\(",
        re.IGNORECASE,
    )

    for line in sql_path.read_text(encoding="utf-8").splitlines():
        use_match = use_re.match(line)
        if use_match:
            current_db = use_match.group(1)
            databases.setdefault(current_db, {})
            continue

        table_match = table_re.match(line)
        if current_db and table_match:
            current_table = table_match.group(1)
            table_lines = []
            continue

        if current_db and current_table:
            if re.match(r"^\s*\)\s*ENGINE=", line, re.IGNORECASE):
                databases[current_db][current_table] = Table(
                    current_db,
                    current_table,
                    tuple(parse_columns(table_lines)),
                )
                current_table = None
                table_lines = []
            else:
                table_lines.append(line)

    return databases


def parse_columns(lines: list[str]) -> list[Column]:
    columns: list[Column] = []
    skip_prefixes = (
        "INDEX ",
        "KEY ",
        "UNIQUE KEY ",
        "PRIMARY KEY ",
        "CONSTRAINT ",
    )
    for raw_line in lines:
        line = raw_line.strip().rstrip(",")
        if not line:
            continue
        upper = line.upper()
        if upper.startswith(skip_prefixes):
            continue

        parts = line.split()
        if len(parts) < 2:
            continue

        name = parts[0].strip("`")
        type_name = parts[1]
        flags: list[str] = []
        if "PRIMARY KEY" in upper:
            flags.append("PK")
        if "NOT NULL" in upper:
            flags.append("NN")
        if "UNIQUE" in upper:
            flags.append("UQ")
        if "AUTO_INCREMENT" in upper:
            flags.append("AI")
        if "DEFAULT" in upper:
            flags.append("DEF")
        return_col = Column(name, type_name, tuple(flags))
        columns.append(return_col)
    return columns


def table_label(table: Table) -> str:
    rows = [
        '<TR><TD BGCOLOR="#24415F" COLSPAN="3"><FONT COLOR="white"><B>'
        + html.escape(table.name)
        + "</B></FONT></TD></TR>"
    ]
    for col in table.columns:
        flags = " ".join(col.flags) if col.flags else " "
        rows.append(
            "<TR>"
            + f'<TD PORT="{html.escape(col.name)}" ALIGN="LEFT"><B>{html.escape(col.name)}</B></TD>'
            + f'<TD ALIGN="LEFT">{html.escape(col.type_name)}</TD>'
            + f'<TD ALIGN="LEFT"><FONT POINT-SIZE="10">{html.escape(flags)}</FONT></TD>'
            + "</TR>"
        )
    return (
        '<<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="6" '
        'COLOR="#6A7A89">'
        + "".join(rows)
        + "</TABLE>>"
    )


def node_id(name: str) -> str:
    return re.sub(r"[^A-Za-z0-9_]", "_", name)


def build_dot(service: str, tables: dict[str, Table], config: dict) -> str:
    lines = [
        "digraph ER {",
        "  graph [rankdir=LR, bgcolor=white, pad=0.35, nodesep=0.55, ranksep=0.9, splines=true];",
        "  node [shape=plain, fontname=Arial];",
        "  edge [fontname=Arial, fontsize=10, color=\"#5D6D7E\", arrowsize=0.7];",
        f'  labelloc="t";',
        f'  label="{config["title"]}";',
        "",
    ]

    for table in tables.values():
        lines.append(f'  {node_id(table.name)} [label={table_label(table)}];')

    for ext_id, table in config.get("external_tables", {}).items():
        lines.append(
            f'  {node_id(ext_id)} [label={table_label(table)}, style="dashed"];'
        )

    lines.append("")
    for relation in config["relations"]:
        style = "dashed"
        color = "#9A6A2F" if relation.external else "#4F6F52"
        lines.append(
            f'  {node_id(relation.src_table)}:{relation.src_col} -> '
            f'{node_id(relation.dst_table)}:{relation.dst_col} '
            f'[label="{html.escape(relation.label)}", style="{style}", color="{color}"];'
        )

    lines.append("}")
    return "\n".join(lines)


def render(dot_path: Path, fmt: str) -> None:
    output_path = dot_path.with_suffix(f".{fmt}")
    subprocess.run(
        ["dot", f"-T{fmt}", str(dot_path), "-o", str(output_path)],
        cwd=ROOT,
        check=True,
    )


def main() -> None:
    databases = parse_bootstrap_sql(SQL_PATH)
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    for service, config in SERVICES.items():
        database = config["database"]
        service_tables = databases.get(database, {})
        if not service_tables:
            raise RuntimeError(f"No tables found for {service} database {database}")

        dot = build_dot(service, service_tables, config)
        dot_path = OUT_DIR / f"{service}-er.dot"
        dot_path.write_text(dot, encoding="utf-8")
        render(dot_path, "svg")
        render(dot_path, "png")
        print(f"wrote {dot_path.with_suffix('.svg')}")
        print(f"wrote {dot_path.with_suffix('.png')}")


if __name__ == "__main__":
    main()
